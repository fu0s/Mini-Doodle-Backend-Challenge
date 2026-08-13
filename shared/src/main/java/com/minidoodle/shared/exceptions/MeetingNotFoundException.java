package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

/**
 * Exception thrown when a requested meeting is not found.
 */
public class MeetingNotFoundException extends SchedulingException {
    
    /**
     * Constructs a new meeting not found exception.
     *
     * @param meetingId the ID of the meeting that was not found
     */
    public MeetingNotFoundException(Long meetingId) {
        super(ErrorCode.MEETING_NOT_FOUND, "Meeting not found with ID: " + meetingId);
    }
    
    /**
     * Constructs a new meeting not found exception with a custom message.
     *
     * @param message custom error message
     */
    public MeetingNotFoundException(String message) {
        super(ErrorCode.MEETING_NOT_FOUND, message);
    }
}
