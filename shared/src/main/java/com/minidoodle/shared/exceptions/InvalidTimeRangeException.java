package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

import java.time.LocalDateTime;

/**
 * Exception thrown when a time range is invalid (end time before or equal to start time).
 */
public class InvalidTimeRangeException extends SchedulingException {
    
    /**
     * Constructs a new invalid time range exception.
     *
     * @param start start time
     * @param end end time
     */
    public InvalidTimeRangeException(LocalDateTime start, LocalDateTime end) {
        super(ErrorCode.INVALID_TIME_RANGE,
              String.format("Invalid time range: end time (%s) must be after start time (%s)", 
                          end, start));
    }
    
    /**
     * Constructs a new invalid time range exception with a custom message.
     *
     * @param message custom error message
     */
    public InvalidTimeRangeException(String message) {
        super(ErrorCode.INVALID_TIME_RANGE, message);
    }
}
