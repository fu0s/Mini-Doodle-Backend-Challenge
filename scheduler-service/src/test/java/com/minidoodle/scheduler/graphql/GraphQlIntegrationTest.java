package com.minidoodle.scheduler.graphql;

import com.minidoodle.scheduler.SchedulerServiceApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.springframework.graphql.execution.ErrorType.BAD_REQUEST;
import static org.springframework.graphql.execution.ErrorType.FORBIDDEN;
import static org.springframework.graphql.execution.ErrorType.NOT_FOUND;

/**
 * End-to-end GraphQL integration test for scheduler-service.
 * <p>
 * Boots the real {@link SchedulerServiceApplication} (web environment against
 * random port), backed by Testcontainers PostgreSQL and Kafka, and drives it
 * through the HTTP endpoint with an {@link HttpGraphQlTester}. Verifies that
 * mutations and queries return the expected shapes, and that domain and
 * validation failures are mapped to typed GraphQL errors plus the stable
 * {@code errorCode} extension by {@link GraphQlExceptionResolver}.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = SchedulerServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
class GraphQlIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.5"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("scheduling.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    HttpGraphQlTester graphQlTester;

    private static final String USERNAME = "gqltest";

    @Test
    void createSlotAndSlotsByUsername_returnExpectedShapes() {
        String id = graphQlTester.document("""
                mutation CreateSlot($input: CreateSlotInput!) {
                    createSlot(input: $input) {
                        id
                        username
                        start
                        end
                        status
                        meetingId
                    }
                }
                """)
                .variable("input", java.util.Map.of(
                        "username", USERNAME,
                        "start", LocalDateTime.of(2026, 8, 18, 9, 0).toString(),
                        "end", LocalDateTime.of(2026, 8, 18, 10, 0).toString(),
                        "status", "FREE"))
                .execute()
                .path("createSlot.username").entity(String.class).isEqualTo(USERNAME)
                .path("createSlot.status").entity(String.class).isEqualTo("FREE")
                .path("createSlot.meetingId").valueIsNull()
                .path("createSlot.start").entity(String.class)
                .isEqualTo(LocalDateTime.of(2026, 8, 18, 9, 0).toString())
                .path("createSlot.id").entity(String.class).get();

        graphQlTester.document("""
                query Slots($username: String!) {
                    slotsByUsername(username: $username) {
                        id
                        username
                        status
                    }
                }
                """)
                .variable("username", USERNAME)
                .execute()
                .path("slotsByUsername[0].id").entity(String.class).isEqualTo(id)
                .path("slotsByUsername[0].username").entity(String.class).isEqualTo(USERNAME);
    }

    @Test
    void createSlotWithEndBeforeStart_isRejectedByBeanValidation() {
        graphQlTester.document("""
                mutation CreateSlot($input: CreateSlotInput!) {
                    createSlot(input: $input) { id }
                }
                """)
                .variable("input", java.util.Map.of(
                        "username", USERNAME,
                        "start", LocalDateTime.of(2026, 8, 18, 11, 0).toString(),
                        "end", LocalDateTime.of(2026, 8, 18, 10, 0).toString(),
                        "status", "FREE"))
                .execute()
                .errors()
                .satisfy(errors -> {
                    org.assertj.core.api.Assertions.assertThat(errors)
                            .anySatisfy(e -> {
                                org.assertj.core.api.Assertions.assertThat(e.getErrorType())
                                        .isEqualTo(BAD_REQUEST);
                                org.assertj.core.api.Assertions.assertThat(e.getExtensions())
                                        .containsEntry("errorCode", "INVALID_INPUT");
                            });
                });
    }

    @Test
    void createMeetingWithoutCoveringFreeSlot_mapsToConflictWithErrorCode() {
        graphQlTester.document("""
                mutation CreateMeeting($input: CreateMeetingInput!) {
                    createMeeting(input: $input) { id }
                }
                """)
                .variable("input", java.util.Map.of(
                        "title", "Home stretch",
                        "participants", java.util.List.of(USERNAME),
                        "start", LocalDateTime.of(2026, 8, 18, 12, 0).toString(),
                        "end", LocalDateTime.of(2026, 8, 18, 13, 0).toString()))
                .execute()
                .errors()
                .satisfy(errors -> {
                    org.assertj.core.api.Assertions.assertThat(errors)
                            .anySatisfy(e -> {
                                org.assertj.core.api.Assertions.assertThat(e.getErrorType())
                                        .isEqualTo(FORBIDDEN);
                                org.assertj.core.api.Assertions.assertThat(e.getExtensions())
                                        .containsEntry("errorCode", "SLOT_CONFLICT");
                            });
                });
    }

    @Test
    void updateSlotForMissingId_mapsToNotFoundWithErrorCode() {
        graphQlTester.document("""
                mutation UpdateSlot($input: UpdateSlotInput!) {
                    updateSlot(input: $input) { id }
                }
                """)
                .variable("input", java.util.Map.of(
                        "slotId", "999999",
                        "start", LocalDateTime.of(2026, 8, 18, 14, 0).toString(),
                        "end", LocalDateTime.of(2026, 8, 18, 15, 0).toString()))
                .execute()
                .errors()
                .satisfy(errors -> {
                    org.assertj.core.api.Assertions.assertThat(errors)
                            .anySatisfy(e -> {
                                org.assertj.core.api.Assertions.assertThat(e.getErrorType())
                                        .isEqualTo(NOT_FOUND);
                                org.assertj.core.api.Assertions.assertThat(e.getExtensions())
                                        .containsEntry("errorCode", "SLOT_NOT_FOUND");
                            });
                });
    }

    @Test
    void createMeetingEndBeforeStart_mapsToInvalidInput() {
        graphQlTester.document("""
                mutation CreateMeeting($input: CreateMeetingInput!) {
                    createMeeting(input: $input) { id }
                }
                """)
                .variable("input", java.util.Map.of(
                        "title", "Backwards",
                        "participants", java.util.List.of(USERNAME),
                        "start", LocalDateTime.of(2026, 8, 18, 16, 0).toString(),
                        "end", LocalDateTime.of(2026, 8, 18, 15, 0).toString()))
                .execute()
                .errors()
                .satisfy(errors -> {
                    org.assertj.core.api.Assertions.assertThat(errors)
                            .anySatisfy(e -> {
                                org.assertj.core.api.Assertions.assertThat(e.getErrorType())
                                        .isEqualTo(BAD_REQUEST);
                                org.assertj.core.api.Assertions.assertThat(e.getExtensions())
                                        .containsEntry("errorCode", "INVALID_INPUT");
                            });
                });
    }
}
