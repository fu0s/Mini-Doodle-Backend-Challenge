package com.minidoodle.scheduler.graphql.validation;

import com.minidoodle.scheduler.graphql.dto.CreateMeetingInput;
import com.minidoodle.scheduler.graphql.dto.CreateSlotInput;
import com.minidoodle.scheduler.graphql.dto.UpdateMeetingInput;
import com.minidoodle.scheduler.graphql.dto.UpdateSlotInput;
import com.minidoodle.shared.constants.SlotStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the bean-validation annotations applied to GraphQL inputs,
 * most importantly the custom {@link ValidTimeRange} constraint which
 * rejects time ranges where end &lt;= start.
 */
class InputValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private final LocalDateTime start = LocalDateTime.of(2026, 8, 15, 9, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 8, 15, 10, 0);

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void createSlotInput_validRange_hasNoViolations() {
        CreateSlotInput input = new CreateSlotInput("alice", start, end, SlotStatus.FREE);
        assertTrue(validator.validate(input).isEmpty());
    }

    @Test
    void createSlotInput_endBeforeStart_isRejected() {
        CreateSlotInput input = new CreateSlotInput("alice", end, start, null);
        var violations = validator.validate(input);
        assertEquals(1, violations.size());
        ConstraintViolation<?> violation = violations.iterator().next();
        assertEquals("end time must be after start time", violation.getMessage());
        assertTrue(violation.getConstraintDescriptor().getAnnotation()
                instanceof ValidTimeRange);
    }

    @Test
    void createSlotInput_endEqualsStart_isRejected() {
        CreateSlotInput input = new CreateSlotInput("alice", start, start, null);
        assertEquals(1, validator.validate(input).size());
    }

    @Test
    void createSlotInput_blankUsername_isRejected() {
        CreateSlotInput input = new CreateSlotInput("  ", start, end, null);
        var violations = validator.validate(input);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    void createSlotInput_missingStartOrEnd_isRejected() {
        CreateSlotInput noStart = new CreateSlotInput("alice", null, end, null);
        var violations = validator.validate(noStart);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("start")));
    }

    @Test
    void updateSlotInput_endBeforeStart_isRejected() {
        UpdateSlotInput input = new UpdateSlotInput(1L, end, start);
        assertEquals(1, validator.validate(input).size());
    }

    @Test
    void createMeetingInput_validRange_andAnnotations_pass() {
        CreateMeetingInput input =
                new CreateMeetingInput("Standup", "daily sync", List.of("alice", "bob"), start, end);
        assertTrue(validator.validate(input).isEmpty());
    }

    @Test
    void createMeetingInput_endBeforeStart_isRejected() {
        CreateMeetingInput input =
                new CreateMeetingInput("Standup", "daily sync", List.of("alice"), end, start);
        assertEquals(1, validator.validate(input).size());
    }

    @Test
    void createMeetingInput_blankTitle_isRejected() {
        CreateMeetingInput input =
                new CreateMeetingInput("  ", "daily sync", List.of("alice"), start, end);
        assertTrue(validator.validate(input).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void createMeetingInput_emptyParticipants_isRejected() {
        CreateMeetingInput input =
                new CreateMeetingInput("Standup", "daily sync", List.of(), start, end);
        assertTrue(validator.validate(input).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("participants")));
    }

    @Test
    void updateMeetingInput_endEqualsStart_isRejected() {
        UpdateMeetingInput input =
                new UpdateMeetingInput(5L, "Retro", null, List.of("bob"), start, start);
        assertEquals(1, validator.validate(input).size());
    }
}