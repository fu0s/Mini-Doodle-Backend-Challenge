package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

import java.time.LocalDateTime;

/**
 * Exception thrown when a participant has conflicting slots during the requested time range.
 * Indicates that one or more participants are not available (already BUSY) for the proposed meeting time.
 */
public class SlotConflictException extends SchedulingException {
    
    /**
     * Constructs a new slot conflict exception.
     *
     * @param username the participant username with the conflict
     * @param start start time of the conflicting range
     * @param end end time of the conflicting range
     */
    public SlotConflictException(String username, LocalDateTime start, LocalDateTime end) {
        super(ErrorCode.SLOT_CONFLICT, 
              String.format("Participant '%s' has conflicting slots between %s and %s", 
                          username, start, end));
    }
    
    /**
     * Constructs a new slot conflict exception with a custom message.
     *
     * @param message custom error message
     */
    public SlotConflictException(String message) {
        super(ErrorCode.SLOT_CONFLICT, message);
    }
}
