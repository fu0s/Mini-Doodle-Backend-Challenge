package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

/**
 * Exception thrown when a requested slot is not found.
 */
public class SlotNotFoundException extends SchedulingException {
    
    /**
     * Constructs a new slot not found exception.
     *
     * @param slotId the ID of the slot that was not found
     */
    public SlotNotFoundException(Long slotId) {
        super(ErrorCode.SLOT_NOT_FOUND, "Slot not found with ID: " + slotId);
    }
    
    /**
     * Constructs a new slot not found exception with a custom message.
     *
     * @param message custom error message
     */
    public SlotNotFoundException(String message) {
        super(ErrorCode.SLOT_NOT_FOUND, message);
    }
}
