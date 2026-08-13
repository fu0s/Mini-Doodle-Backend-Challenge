package com.minidoodle.shared.service;

import com.minidoodle.shared.domain.Slot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for slot splitting operations.
 * Handles the logic of splitting existing FREE slots when BUSY periods are created,
 * and properly tagging slots with meetingId references.
 * This interface allows for different splitting strategies and is swappable/mockable.
 */
public interface SlotSplitter {
    
    /**
     * Splits existing FREE slots for a user when a new BUSY period is created.
     * For each FREE slot that overlaps with the busy period:
     * - If fully covered, mark as BUSY
     * - If partially covered, split into FREE/BUSY/FREE segments as needed
     * All created BUSY slots are tagged with the provided meetingId.
     *
     * @param username the user whose slots to split
     * @param busyStart start time of the busy period
     * @param busyEnd end time of the busy period
     * @param meetingId the meeting ID to associate with created BUSY slots (may be null)
     * @return list of newly created or modified slots
     */
    List<Slot> splitSlotsForBusyPeriod(String username, LocalDateTime busyStart, 
                                       LocalDateTime busyEnd, Long meetingId);
    
    /**
     * Frees slots linked to a specific meeting, converting them back to FREE status.
     * Used when a meeting is deleted or updated.
     *
     * @param meetingId the meeting ID whose linked slots should be freed
     * @return list of freed slots
     */
    List<Slot> freeSlotsForMeeting(Long meetingId);
}
