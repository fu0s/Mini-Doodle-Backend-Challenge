package com.minidoodle.shared.service.impl;

import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import com.minidoodle.shared.service.SlotSplitter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of SlotSplitter that handles splitting FREE slots when BUSY periods are created.
 * A FREE slot covering a busy period is replaced by:
 * - one BUSY slot (exact duration, tagged with meetingId)
 * - zero, one, or two FREE remainder slots (meetingId null)
 * Also converts BUSY slots back to FREE when a meeting is deleted or updated,
 * merging adjacent FREE remainder slots to keep the user's slot list clean.
 */
@Component
public class SlotSplitterImpl implements SlotSplitter {

    private final SlotRepository slotRepository;

    public SlotSplitterImpl(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Override
    @Transactional
    public List<Slot> splitSlotsForBusyPeriod(String username, LocalDateTime busyStart, LocalDateTime busyEnd, Long meetingId) {
        List<Slot> result = new ArrayList<>();

        // Find all FREE slots that fully cover the busy period
        List<SlotEntity> overlappingFreeSlots = slotRepository.findFreeSlotsCoveringRange(username, busyStart, busyEnd);

        for (SlotEntity freeSlot : overlappingFreeSlots) {
            // Delete the original FREE slot
            slotRepository.delete(freeSlot);

            // Split into parts
            List<Slot> splitParts = splitSingleSlot(freeSlot, busyStart, busyEnd, meetingId);

            // Save each part
            for (Slot part : splitParts) {
                SlotEntity entity = new SlotEntity();
                entity.setUsername(part.getUsername());
                entity.setStartTime(part.getStart());
                entity.setEndTime(part.getEnd());
                entity.setStatus(part.getStatus());
                entity.setMeetingId(part.getMeetingId());
                SlotEntity saved = slotRepository.save(entity);
                result.add(toSlot(saved));
            }
        }

        return result;
    }

    /**
     * Splits a single FREE slot around a busy period.
     * Returns a list containing:
     * - One BUSY slot (exact duration, tagged with meetingId)
     * - Zero, one, or two FREE remainder slots
     *
     * @param freeSlot the FREE slot that fully covers the busy period
     * @param busyStart start time of the busy period
     * @param busyEnd end time of the busy period
     * @param meetingId the meeting ID to tag on the BUSY slot
     * @return the split parts (BUSY slot first, then FREE remainders)
     */
    private List<Slot> splitSingleSlot(SlotEntity freeSlot, LocalDateTime busyStart, LocalDateTime busyEnd, Long meetingId) {
        List<Slot> parts = new ArrayList<>();

        LocalDateTime slotStart = freeSlot.getStartTime();
        LocalDateTime slotEnd = freeSlot.getEndTime();

        // Create the BUSY slot for the meeting duration
        Slot busySlot = new Slot(null, freeSlot.getUsername(), busyStart, busyEnd, SlotStatus.BUSY, meetingId);
        parts.add(busySlot);

        // Check for FREE slot before the busy period
        if (slotStart.isBefore(busyStart)) {
            Slot beforeFree = new Slot(null, freeSlot.getUsername(), slotStart, busyStart, SlotStatus.FREE, null);
            parts.add(beforeFree);
        }

        // Check for FREE slot after the busy period
        if (slotEnd.isAfter(busyEnd)) {
            Slot afterFree = new Slot(null, freeSlot.getUsername(), busyEnd, slotEnd, SlotStatus.FREE, null);
            parts.add(afterFree);
        }

        return parts;
    }

    @Override
    @Transactional
    public List<Slot> freeSlotsForMeeting(Long meetingId) {
        List<Slot> freedSlots = new ArrayList<>();

        // Find all slots linked to this meeting
        List<SlotEntity> linkedSlots = slotRepository.findByMeetingId(meetingId);

        for (SlotEntity slot : linkedSlots) {
            // Clear the meetingId and set status to FREE
            slot.setMeetingId(null);
            slot.setStatus(SlotStatus.FREE);
            SlotEntity saved = slotRepository.save(slot);
            freedSlots.add(toSlot(saved));
        }

        // Try to merge adjacent FREE slots
        mergeAdjacentFreeSlots(freedSlots);

        return freedSlots;
    }

    /**
     * Merges adjacent FREE slots (one ends exactly where the other starts) for each
     * affected user, keeping the user's slot list clean after freeing meeting slots.
     *
     * @param freedSlots the slots that were just freed
     */
    private void mergeAdjacentFreeSlots(List<Slot> freedSlots) {
        Set<String> usernames = freedSlots.stream()
                .map(Slot::getUsername)
                .filter(username -> username != null)
                .collect(Collectors.toSet());

        for (String username : usernames) {
            boolean merged = true;

            while (merged) {
                merged = false;

                List<SlotEntity> userFreeSlots = slotRepository.findByUsernameAndStatus(username, SlotStatus.FREE)
                        .stream()
                        .sorted(Comparator.comparing(SlotEntity::getStartTime))
                        .collect(Collectors.toList());

                for (int i = 0; i < userFreeSlots.size() - 1; i++) {
                    SlotEntity current = userFreeSlots.get(i);
                    SlotEntity next = userFreeSlots.get(i + 1);

                    if (current.getEndTime().equals(next.getStartTime())) {
                        // Extend the earlier slot and drop the adjacent one
                        current.setEndTime(next.getEndTime());
                        slotRepository.save(current);
                        slotRepository.delete(next);
                        merged = true;
                        break;
                    }
                }
            }
        }
    }

    private Slot toSlot(SlotEntity entity) {
        return new Slot(entity.getId(), entity.getUsername(), entity.getStartTime(),
                entity.getEndTime(), entity.getStatus(), entity.getMeetingId());
    }
}