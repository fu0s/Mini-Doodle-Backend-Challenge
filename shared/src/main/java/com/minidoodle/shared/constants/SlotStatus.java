package com.minidoodle.shared.constants;

/**
 * Slot availability status enumeration.
 * Represents whether a time slot is available (FREE) or occupied (BUSY).
 */
public enum SlotStatus {
    /**
     * Slot is available for scheduling.
     */
    FREE,
    
    /**
     * Slot is occupied and not available.
     * May be linked to a meeting via meetingId or manually marked busy.
     */
    BUSY
}
