package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

/**
 * Exception thrown when a participants list is invalid (empty, null, or malformed).
 */
public class InvalidParticipantsException extends SchedulingException {
    
    /**
     * Constructs a new invalid participants exception.
     *
     * @param message detail message explaining why the participants list is invalid
     */
    public InvalidParticipantsException(String message) {
        super(ErrorCode.INVALID_PARTICIPANTS, message);
    }
}
