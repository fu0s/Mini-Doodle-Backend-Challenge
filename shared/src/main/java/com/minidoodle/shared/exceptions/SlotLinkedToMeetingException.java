package com.minidoodle.shared.exceptions;

import com.minidoodle.shared.constants.ErrorCode;

/**
 * Exception thrown when attempting to modify or delete a busy slot that is linked to an active meeting.
 * Meeting-linked slots should be managed through meeting lifecycle operations.
 */
public class SlotLinkedToMeetingException extends SchedulingException {
    
    /**
     * Constructs a new slot linked to meeting exception.
     *
     * @param slotId the ID of the linked slot
     * @param meetingId the ID of the meeting the slot is linked to
     */
    public SlotLinkedToMeetingException(Long slotId, Long meetingId) {
        super(ErrorCode.SLOT_LINKED_TO_MEETING,
              String.format("Slot %d is linked to meeting %d and cannot be modified directly", 
                          slotId, meetingId));
    }
    
    /**
     * Constructs a new slot linked to meeting exception with a custom message.
     *
     * @param message custom error message
     */
    public SlotLinkedToMeetingException(String message) {
        super(ErrorCode.SLOT_LINKED_TO_MEETING, message);
    }
}
