package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the trigger semantics of the {@link SchedulingException} hierarchy:
 * each exception carries the correct {@link ErrorCode} and a descriptive,
 * participant/slot-aware message built from its constructor arguments.
 */
class SchedulingExceptionsTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 8, 15, 9, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 8, 15, 10, 0);

    @Test
    void schedulingException_exposesErrorCode() {
        SchedulingException ex = new SchedulingException(ErrorCode.SLOT_CONFLICT, "boom") {
        };
        assertEquals(ErrorCode.SLOT_CONFLICT, ex.getErrorCode());
        assertEquals("boom", ex.getMessage());
        assertEquals(null, ex.getCause());
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void schedulingException_withCause_keepsCauseChain() {
        IllegalStateException cause = new IllegalStateException("root");
        SchedulingException ex = new SchedulingException(ErrorCode.MEETING_EVENT_PROCESSING, "wrapped", cause) {
        };
        assertEquals(ErrorCode.MEETING_EVENT_PROCESSING, ex.getErrorCode());
        assertEquals("wrapped", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void slotNotFoundException_hasSlotNotFoundCodeAndIdInMessage() {
        SlotNotFoundException ex = new SlotNotFoundException(42L);
        assertEquals(ErrorCode.SLOT_NOT_FOUND, ex.getErrorCode());
        assertEquals("Slot not found with ID: 42", ex.getMessage());
    }

    @Test
    void slotNotFoundException_customMessage_preserved() {
        SlotNotFoundException ex = new SlotNotFoundException("no such slot");
        assertEquals(ErrorCode.SLOT_NOT_FOUND, ex.getErrorCode());
        assertEquals("no such slot", ex.getMessage());
    }

    @Test
    void meetingNotFoundException_hasMeetingNotFoundCodeAndIdInMessage() {
        MeetingNotFoundException ex = new MeetingNotFoundException(7L);
        assertEquals(ErrorCode.MEETING_NOT_FOUND, ex.getErrorCode());
        assertEquals("Meeting not found with ID: 7", ex.getMessage());
    }

    @Test
    void invalidTimeRangeException_hasInvalidTimeRangeCodeAndBoundsInMessage() {
        InvalidTimeRangeException ex = new InvalidTimeRangeException(start, end);
        assertEquals(ErrorCode.INVALID_TIME_RANGE, ex.getErrorCode());
        assertTrue(ex.getMessage().contains(end.toString()));
        assertTrue(ex.getMessage().contains(start.toString()));
    }

    @Test
    void slotConflictException_hasSlotConflictCodeAndUsernameAndBoundsInMessage() {
        SlotConflictException ex = new SlotConflictException("alice", start, end);
        assertEquals(ErrorCode.SLOT_CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("alice"));
        assertTrue(ex.getMessage().contains(start.toString()));
        assertTrue(ex.getMessage().contains(end.toString()));
    }

    @Test
    void slotConflictException_customMessage_usesAggregatedNames() {
        SlotConflictException ex = new SlotConflictException("Participants not available: [alice, bob]");
        assertEquals(ErrorCode.SLOT_CONFLICT, ex.getErrorCode());
        assertEquals("Participants not available: [alice, bob]", ex.getMessage());
    }

    @Test
    void slotLinkedToMeetingException_hasLinkedCodeAndBothIdsInMessage() {
        SlotLinkedToMeetingException ex = new SlotLinkedToMeetingException(3L, 100L);
        assertEquals(ErrorCode.SLOT_LINKED_TO_MEETING, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("3"));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void invalidParticipantsException_hasInvalidParticipantsCodeAndReasonInMessage() {
        InvalidParticipantsException ex = new InvalidParticipantsException("Participants list cannot be empty");
        assertEquals(ErrorCode.INVALID_PARTICIPANTS, ex.getErrorCode());
        assertEquals("Participants list cannot be empty", ex.getMessage());
    }

    @Test
    void meetingEventProcessingException_hasProcessingCodeAndCause() {
        IllegalStateException cause = new IllegalStateException("kafka down");
        MeetingEventProcessingException ex =
                new MeetingEventProcessingException("Failed to process event", cause);
        assertEquals(ErrorCode.MEETING_EVENT_PROCESSING, ex.getErrorCode());
        assertEquals("Failed to process event", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}