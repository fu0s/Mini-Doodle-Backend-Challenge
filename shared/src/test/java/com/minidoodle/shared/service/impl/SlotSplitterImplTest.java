package com.minidoodle.shared.service.impl;

import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotSplitterImplTest {

    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private SlotSplitterImpl slotSplitter;

    private LocalDateTime slotStart;
    private LocalDateTime mid;
    private LocalDateTime slotEnd;
    private SlotEntity freeSlotEntity;

    @BeforeEach
    void setUp() {
        slotStart = LocalDateTime.of(2026, 1, 1, 9, 0);
        mid = slotStart.plusHours(1);
        slotEnd = slotStart.plusHours(2);
        freeSlotEntity = new SlotEntity(1L, "user1", slotStart, slotEnd, SlotStatus.FREE, null);
    }

    @Test
    void splitSlotsForBusyPeriod_FullCoverage_ReturnsSingleBusySlot() {
        when(slotRepository.findFreeSlotsCoveringRange("user1", slotStart, slotEnd))
                .thenReturn(List.of(freeSlotEntity));
        when(slotRepository.save(any(SlotEntity.class))).thenAnswer(inv -> {
            SlotEntity e = inv.getArgument(0);
            return new SlotEntity(100L, e.getUsername(), e.getStartTime(), e.getEndTime(),
                    e.getStatus(), e.getMeetingId());
        });

        List<Slot> result = slotSplitter.splitSlotsForBusyPeriod("user1", slotStart, slotEnd, 42L);

        assertEquals(1, result.size());
        Slot busy = result.get(0);
        assertEquals(SlotStatus.BUSY, busy.getStatus());
        assertEquals(42L, busy.getMeetingId());
        assertEquals(slotStart, busy.getStart());
        assertEquals(slotEnd, busy.getEnd());
        verify(slotRepository).delete(freeSlotEntity);
    }

    @Test
    void splitSlotsForBusyPeriod_MiddleOverlap_ReturnsBusyWithTwoFreeRemainders() {
        LocalDateTime busyStart = mid.minusMinutes(30);
        LocalDateTime busyEnd = mid.plusMinutes(30);

        when(slotRepository.findFreeSlotsCoveringRange("user1", busyStart, busyEnd))
                .thenReturn(List.of(freeSlotEntity));
        when(slotRepository.save(any(SlotEntity.class))).thenAnswer(inv -> {
            SlotEntity e = inv.getArgument(0);
            return new SlotEntity(100L, e.getUsername(), e.getStartTime(), e.getEndTime(),
                    e.getStatus(), e.getMeetingId());
        });

        List<Slot> result = slotSplitter.splitSlotsForBusyPeriod("user1", busyStart, busyEnd, 42L);

        assertEquals(3, result.size());

        Slot busy = result.get(0);
        assertEquals(SlotStatus.BUSY, busy.getStatus());
        assertEquals(42L, busy.getMeetingId());
        assertEquals(busyStart, busy.getStart());
        assertEquals(busyEnd, busy.getEnd());

        Slot before = result.get(1);
        assertEquals(SlotStatus.FREE, before.getStatus());
        assertNull(before.getMeetingId());
        assertEquals(slotStart, before.getStart());
        assertEquals(busyStart, before.getEnd());

        Slot after = result.get(2);
        assertEquals(SlotStatus.FREE, after.getStatus());
        assertNull(after.getMeetingId());
        assertEquals(busyEnd, after.getStart());
        assertEquals(slotEnd, after.getEnd());
    }

    @Test
    void splitSlotsForBusyPeriod_TrailingOverlap_ReturnsBusyAndBeforeRemainder() {
        LocalDateTime busyStart = mid;
        LocalDateTime busyEnd = slotEnd;

        when(slotRepository.findFreeSlotsCoveringRange("user1", busyStart, busyEnd))
                .thenReturn(List.of(freeSlotEntity));
        when(slotRepository.save(any(SlotEntity.class))).thenAnswer(inv -> {
            SlotEntity e = inv.getArgument(0);
            return new SlotEntity(100L, e.getUsername(), e.getStartTime(), e.getEndTime(),
                    e.getStatus(), e.getMeetingId());
        });

        List<Slot> result = slotSplitter.splitSlotsForBusyPeriod("user1", busyStart, busyEnd, 42L);

        assertEquals(2, result.size());
        assertEquals(SlotStatus.BUSY, result.get(0).getStatus());
        assertEquals(SlotStatus.FREE, result.get(1).getStatus());
        assertEquals(slotStart, result.get(1).getStart());
        assertEquals(busyStart, result.get(1).getEnd());
    }

    @Test
    void splitSlotsForBusyPeriod_NoCoveringSlots_ReturnsEmptyList() {
        when(slotRepository.findFreeSlotsCoveringRange("user1", slotStart, slotEnd))
                .thenReturn(List.of());

        List<Slot> result = slotSplitter.splitSlotsForBusyPeriod("user1", slotStart, slotEnd, 42L);

        assertTrue(result.isEmpty());
        verify(slotRepository, never()).delete(any(SlotEntity.class));
    }

    @Test
    void freeSlotsForMeeting_ResetsAllLinkedSlotsToFree() {
        SlotEntity busy1 = new SlotEntity(1L, "user1", slotStart, mid, SlotStatus.BUSY, 42L);
        SlotEntity busy2 = new SlotEntity(2L, "user2", mid, slotEnd, SlotStatus.BUSY, 42L);

        when(slotRepository.findByMeetingId(42L)).thenReturn(List.of(busy1, busy2));
        when(slotRepository.save(any(SlotEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(slotRepository.findByUsernameAndStatus(anyString(), eq(SlotStatus.FREE))).thenReturn(List.of());

        List<Slot> result = slotSplitter.freeSlotsForMeeting(42L);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> s.getStatus() == SlotStatus.FREE));
        assertTrue(result.stream().allMatch(s -> s.getMeetingId() == null));
    }

    @Test
    void freeSlotsForMeeting_MergesAdjacentFreeSlots() {
        SlotEntity freedSlot = new SlotEntity(1L, "user1", slotStart, mid, SlotStatus.BUSY, 42L);
        SlotEntity existingFree = new SlotEntity(2L, "user1", mid, slotEnd, SlotStatus.FREE, null);

        when(slotRepository.findByMeetingId(42L)).thenReturn(List.of(freedSlot));
        when(slotRepository.save(any(SlotEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(slotRepository.findByUsernameAndStatus("user1", SlotStatus.FREE))
                .thenReturn(new ArrayList<>(List.of(freedSlot, existingFree)));

        List<Slot> result = slotSplitter.freeSlotsForMeeting(42L);

        assertEquals(1, result.size());
        verify(slotRepository).delete(existingFree);
        verify(slotRepository, atLeast(2)).save(any(SlotEntity.class));
        assertEquals(slotEnd, freedSlot.getEndTime());
    }
}