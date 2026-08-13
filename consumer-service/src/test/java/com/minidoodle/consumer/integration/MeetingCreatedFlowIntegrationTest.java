package com.minidoodle.consumer.integration;

import com.minidoodle.consumer.ConsumerServiceApplication;
import com.minidoodle.shared.config.SharedKafkaProperties;
import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.domain.Meeting;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import com.minidoodle.shared.service.MeetingEventPublisher;
import com.minidoodle.shared.service.MeetingService;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full integration test for the
 * {@code create-meeting -> publish -> consume -> slot-update (meetingId set)}
 * flow.
 * <p>
 * Boots the real {@link ConsumerServiceApplication} context against a
 * Testcontainers PostgreSQL and a single-node KRaft Kafka broker. A producer
 * {@link KafkaTemplate} plus a real Kafka-backed {@link MeetingEventPublisher}
 * are supplied via a nested {@code @TestConfiguration}; this mirrors the
 * scheduler's publisher wiring without importing the repackaged
 * scheduler-service artifact as a test dependency.
 * <p>
 * Covers the full meeting lifecycle: the {@code create-meeting -> publish ->
 * consume -> async slot-split} pipeline (asynchronous, asserted with
 * Awaitility), plus the synchronous update/delete cascades that free the
 * meeting's slots back to {@code FREE}.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = ConsumerServiceApplication.class)
@Import(MeetingCreatedFlowIntegrationTest.PublisherConfig.class)
class MeetingCreatedFlowIntegrationTest {

    private static final String USERNAME = "alice";
    private static final String UPDATE_USERNAME = "update-user";
    private static final String DELETE_USERNAME = "delete-user";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("doodle")
            .withUsername("doodle")
            .withPassword("doodle");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.5"));

    @DynamicPropertySource
    static void datasourceAndKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("scheduling.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private SlotRepository slotRepository;

    @Test
    void meetingCreationPublishesEventThatMarksParticipantSlotBusyWithMeetingId() {
        LocalDateTime dayStart = LocalDateTime.of(2026, 8, 15, 8, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 8, 15, 20, 0);
        slotRepository.save(new SlotEntity(null, USERNAME, dayStart, dayEnd,
                SlotStatus.FREE, null));

        LocalDateTime start = LocalDateTime.of(2026, 8, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 15, 11, 0);

        Meeting meeting = meetingService.createMeeting(
                "Team sync", "Weekly", List.of(USERNAME), start, end);

        Long meetingId = meeting.getId();
        assertThat(meetingId).isNotNull();

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    List<SlotEntity> busy = slotRepository.findByUsernameAndMeetingId(
                            USERNAME, meetingId);
                    assertThat(busy).hasSize(1);
                    assertThat(busy.get(0).getStatus()).isEqualTo(SlotStatus.BUSY);
                    assertThat(busy.get(0).getMeetingId()).isEqualTo(meetingId);
                });

        List<SlotEntity> all = slotRepository.findByUsername(USERNAME);
        assertThat(all).hasSize(3);
        assertThat(all)
                .filteredOn(s -> s.getStatus() == SlotStatus.BUSY)
                .singleElement()
                .satisfies(s -> assertThat(s.getMeetingId()).isEqualTo(meetingId));
    }

    @Test
    void updatingMeetingFreesOldSlotAndTagsNewBusySlotWithMeetingId() {
        String username = UPDATE_USERNAME;
        LocalDateTime dayStart = LocalDateTime.of(2026, 8, 16, 8, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 8, 16, 20, 0);
        slotRepository.save(new SlotEntity(null, username, dayStart, dayEnd,
                SlotStatus.FREE, null));

        LocalDateTime firstStart = LocalDateTime.of(2026, 8, 16, 10, 0);
        LocalDateTime firstEnd = LocalDateTime.of(2026, 8, 16, 11, 0);

        Meeting meeting = meetingService.createMeeting(
                "Sync", "First slot", List.of(username), firstStart, firstEnd);
        Long meetingId = meeting.getId();

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    List<SlotEntity> busy = slotRepository.findByUsernameAndMeetingId(
                            username, meetingId);
                    assertThat(busy).hasSize(1);
                });

        LocalDateTime secondStart = LocalDateTime.of(2026, 8, 16, 13, 0);
        LocalDateTime secondEnd = LocalDateTime.of(2026, 8, 16, 14, 0);

        meetingService.updateMeeting(meetingId, "Resync", "Second slot",
                List.of(username), secondStart, secondEnd);

        List<SlotEntity> newlyBusy = slotRepository.findByUsernameAndMeetingId(
                username, meetingId);
        assertThat(newlyBusy).hasSize(1);
        assertThat(newlyBusy.get(0).getStatus()).isEqualTo(SlotStatus.BUSY);
        assertThat(newlyBusy.get(0).getStartTime()).isEqualTo(secondStart);
        assertThat(newlyBusy.get(0).getEndTime()).isEqualTo(secondEnd);

        assertThat(slotRepository.findByUsernameAndMeetingId(username, meetingId))
                .noneMatch(s -> s.getStartTime().equals(firstStart));
    }

    @Test
    void deletingMeetingFreesAllSlotsTaggedWithThatMeetingId() {
        String username = DELETE_USERNAME;
        LocalDateTime dayStart = LocalDateTime.of(2026, 8, 17, 8, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 8, 17, 20, 0);
        slotRepository.save(new SlotEntity(null, username, dayStart, dayEnd,
                SlotStatus.FREE, null));

        LocalDateTime start = LocalDateTime.of(2026, 8, 17, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 17, 11, 0);

        Meeting meeting = meetingService.createMeeting(
                "Sync", "To be deleted", List.of(username), start, end);
        Long meetingId = meeting.getId();

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    List<SlotEntity> busy = slotRepository.findByUsernameAndMeetingId(
                            username, meetingId);
                    assertThat(busy).hasSize(1);
                });

        meetingService.deleteMeeting(meetingId);

        assertThat(slotRepository.findByUsernameAndMeetingId(username, meetingId)).isEmpty();
        List<SlotEntity> all = slotRepository.findByUsername(username);
        assertThat(all).allMatch(s -> s.getStatus() == SlotStatus.FREE);
        assertThat(all).allMatch(s -> s.getMeetingId() == null);
    }

    /**
     * Provides a producer-side {@link KafkaTemplate} and a Kafka-backed
     * {@link MeetingEventPublisher}. The dedicated qualifiers avoid ambiguity
     * with the consumer's {@code dltKafkaTemplate} (same generic type) and keep
     * the only {@link MeetingEventPublisher} bean resolvable by the shared
     * {@code MeetingService}.
     */
    @TestConfiguration
    static class PublisherConfig {

        private final SharedKafkaProperties sharedKafkaProperties;

        PublisherConfig(SharedKafkaProperties sharedKafkaProperties) {
            this.sharedKafkaProperties = sharedKafkaProperties;
        }

        @Bean
        ProducerFactory<String, MeetingCreatedEvent> integrationProducerFactory() {
            return new DefaultKafkaProducerFactory<>(
                    Map.of(
                            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                            sharedKafkaProperties.getBootstrapServers(),
                            ProducerConfig.ACKS_CONFIG, "all",
                            ProducerConfig.LINGER_MS_CONFIG, "5"
                    ),
                    new StringSerializer(),
                    new JsonSerializer<>()
            );
        }

        @Bean
        KafkaTemplate<String, MeetingCreatedEvent> integrationMeetingEventTemplate() {
            return new KafkaTemplate<>(integrationProducerFactory());
        }

        @Bean
        MeetingEventPublisher integrationMeetingEventPublisher(
                @Qualifier("integrationMeetingEventTemplate")
                KafkaTemplate<String, MeetingCreatedEvent> template) {
            return new MeetingEventPublisher() {
                @Override
                public void publishMeetingCreated(MeetingCreatedEvent event) {
                    template.send(
                            sharedKafkaProperties.getTopicName(),
                            sharedKafkaProperties.getEventType(),
                            event);
                }
            };
        }
    }
}
