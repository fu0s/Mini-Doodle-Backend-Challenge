package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

/**
 * Thrown when a meeting event cannot be processed successfully.
 */
public class MeetingEventProcessingException extends SchedulingException {

    public MeetingEventProcessingException(String message, Throwable cause) {
        super(ErrorCode.MEETING_EVENT_PROCESSING, message, cause);
    }
}
