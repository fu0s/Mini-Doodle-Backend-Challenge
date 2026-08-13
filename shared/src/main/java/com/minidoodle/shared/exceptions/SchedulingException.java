package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

/**
 * Base unchecked exception for all scheduling-related errors.
 * All domain exceptions extend this class and carry an error code.
 */
public abstract class SchedulingException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    /**
     * Constructs a new scheduling exception.
     *
     * @param errorCode the error code identifying the type of error
     * @param message detail message
     */
    protected SchedulingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Constructs a new scheduling exception with a cause.
     *
     * @param errorCode the error code identifying the type of error
     * @param message detail message
     * @param cause the cause
     */
    protected SchedulingException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Returns the error code associated with this exception.
     *
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
