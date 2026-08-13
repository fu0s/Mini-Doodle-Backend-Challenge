package com.minidoodle.shared.service;

import com.minidoodle.shared.domain.Slot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for slot management operations.
 * Implementations handle slot creation, updates, deletion, and queries.
 */
public interface SlotService {
    
    /**
     * Creates a new slot for a user.
     *
     * @param username the user who owns the slot
     * @param start start time of the slot
     * @param end end time of the slot
     * @return the created slot
     * @throws com.minidoodle.shared.exceptions.InvalidTimeRangeException if end <= start
     */
    Slot createSlot(String username, LocalDateTime start, LocalDateTime end);
    
    /**
     * Updates an existing slot.
     *
     * @param slotId the ID of the slot to update
     * @param start new start time
     * @param end new end time
     * @return the updated slot
     * @throws com.minidoodle.shared.exceptions.SlotNotFoundException if slot not found
     * @throws com.minidoodle.shared.exceptions.SlotLinkedToMeetingException if slot is linked to a meeting
     * @throws com.minidoodle.shared.exceptions.InvalidTimeRangeException if end <= start
     */
    Slot updateSlot(Long slotId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Deletes a slot.
     *
     * @param slotId the ID of the slot to delete
     * @throws com.minidoodle.shared.exceptions.SlotNotFoundException if slot not found
     * @throws com.minidoodle.shared.exceptions.SlotLinkedToMeetingException if slot is linked to a meeting
     */
    void deleteSlot(Long slotId);
    
    /**
     * Retrieves all slots for a specific user.
     *
     * @param username the username to query
     * @return list of slots owned by the user
     */
    List<Slot> getSlotsByUsername(String username);
    
    /**
     * Retrieves slots for a user within a time range.
     *
     * @param username the username to query
     * @param start start of the time range
     * @param end end of the time range
     * @return list of slots within the specified range
     */
    List<Slot> getSlotsByUsernameAndTimeRange(String username, LocalDateTime start, LocalDateTime end);
}
