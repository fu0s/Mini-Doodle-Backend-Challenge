package com.minidoodle.shared.constants;

/**
 * Error codes for scheduling exceptions.
 * Provides consistent error identification across the application.
 */
public enum ErrorCode {
    /**
     * Slot with the specified ID was not found.
     */
    SLOT_NOT_FOUND,
    
    /**
     * Meeting with the specified ID was not found.
     */
    MEETING_NOT_FOUND,
    
    /**
     * Participant has conflicting slots during the requested time range.
     */
    SLOT_CONFLICT,
    
    /**
     * Attempt to modify/delete a busy slot that is linked to an active meeting.
     */
    SLOT_LINKED_TO_MEETING,
    
    /**
     * Invalid time range (end time before or equal to start time).
     */
    INVALID_TIME_RANGE,
    
    /**
     * Invalid participants list (empty, null, or malformed).
     */
    INVALID_PARTICIPANTS
}
