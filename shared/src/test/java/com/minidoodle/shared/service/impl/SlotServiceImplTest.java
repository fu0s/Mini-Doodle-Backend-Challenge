package com.minidoodle.shared.service.impl;

import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.exceptions.InvalidTimeRangeException;
import com.minidoodle.shared.exceptions.SlotLinkedToMeetingException;
import com.minidoodle.shared.exceptions.SlotNotFoundException;
import com.minidoodle.shared.mapper.SlotMapper;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotServiceImplTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private SlotMapper slotMapper;

    @InjectMocks
    private SlotServiceImpl slotService;

    private LocalDateTime now;
    private LocalDateTime later;
    private SlotEntity freeSlotEntity;
    private Slot freeSlot;
    private SlotEntity busySlotEntity;
    private Slot busySlot;
    private SlotEntity meetingLinkedSlotEntity;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        later = now.plusHours(1);
        
        freeSlotEntity = new SlotEntity(1L, "user1", now, later, SlotStatus.FREE, null);
        freeSlot = new Slot(1L, "user1", now, later, SlotStatus.FREE, null);
        
        busySlotEntity = new SlotEntity(2L, "user1", now, later, SlotStatus.BUSY, null);
        busySlot = new Slot(2L, "user1", now, later, SlotStatus.BUSY, null);

        meetingLinkedSlotEntity = new SlotEntity(3L, "user1", now, later, SlotStatus.BUSY, 100L);
    }

    @Test
    void createSlot_Free_ReturnsCreatedSlot() {
        when(slotRepository.save(any(SlotEntity.class))).thenReturn(freeSlotEntity);
        when(slotMapper.toDomain(freeSlotEntity)).thenReturn(freeSlot);
        
        Slot result = slotService.createSlot("user1", now, later, SlotStatus.FREE);
        
        assertNotNull(result);
        assertEquals(freeSlot, result);
        verify(slotRepository).save(any(SlotEntity.class));
    }

    @Test
    void createSlot_ManuallyBusy_ReturnsCreatedSlotWithNoMeetingId() {
        when(slotRepository.save(any(SlotEntity.class))).thenReturn(busySlotEntity);
        when(slotMapper.toDomain(busySlotEntity)).thenReturn(busySlot);
        
        Slot result = slotService.createSlot("user1", now, later, SlotStatus.BUSY);
        
        assertNotNull(result);
        assertEquals(SlotStatus.BUSY, result.getStatus());
        assertNull(result.getMeetingId());
    }

    @Test
    void createSlot_NullStatus_DefaultsToFree() {
        when(slotRepository.save(any(SlotEntity.class))).thenReturn(freeSlotEntity);
        when(slotMapper.toDomain(freeSlotEntity)).thenReturn(freeSlot);
        
        Slot result = slotService.createSlot("user1", now, later, null);
        
        assertNotNull(result);
        assertEquals(freeSlot, result);
    }

    @Test
    void createSlot_InvalidTimeRange_ThrowsException() {
        assertThrows(InvalidTimeRangeException.class, () -> {
            slotService.createSlot("user1", later, now, SlotStatus.FREE);
        });
    }

    @Test
    void updateSlot_FreeSlot_UpdatesAndReturnsSlot() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlotEntity));
        when(slotRepository.save(freeSlotEntity)).thenReturn(freeSlotEntity);
        when(slotMapper.toDomain(freeSlotEntity)).thenReturn(freeSlot);

        Slot result = slotService.updateSlot(1L, now, later);

        assertNotNull(result);
        assertEquals(freeSlot, result);
    }

    @Test
    void updateSlot_MeetingLinkedSlot_ThrowsSlotLinkedToMeetingException() {
        when(slotRepository.findById(3L)).thenReturn(Optional.of(meetingLinkedSlotEntity));

        assertThrows(SlotLinkedToMeetingException.class, () -> {
            slotService.updateSlot(3L, now, later);
        });
        verify(slotRepository, never()).save(any(SlotEntity.class));
    }

    @Test
    void updateSlot_ManuallyBusySlot_IsAllowed() {
        when(slotRepository.findById(2L)).thenReturn(Optional.of(busySlotEntity));
        when(slotRepository.save(busySlotEntity)).thenReturn(busySlotEntity);
        when(slotMapper.toDomain(busySlotEntity)).thenReturn(busySlot);

        Slot result = slotService.updateSlot(2L, now, later);

        assertNotNull(result);
        assertEquals(busySlot, result);
    }

    @Test
    void updateSlot_NotFound_ThrowsSlotNotFoundException() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SlotNotFoundException.class, () -> {
            slotService.updateSlot(99L, now, later);
        });
    }

    @Test
    void updateSlot_InvalidTimeRange_ThrowsException() {
        assertThrows(InvalidTimeRangeException.class, () -> {
            slotService.updateSlot(1L, later, now);
        });
    }

    @Test
    void deleteSlot_FreeSlot_DeletesSlot() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlotEntity));

        slotService.deleteSlot(1L);

        verify(slotRepository).delete(freeSlotEntity);
    }

    @Test
    void deleteSlot_MeetingLinkedSlot_ThrowsSlotLinkedToMeetingException() {
        when(slotRepository.findById(3L)).thenReturn(Optional.of(meetingLinkedSlotEntity));

        assertThrows(SlotLinkedToMeetingException.class, () -> {
            slotService.deleteSlot(3L);
        });
        verify(slotRepository, never()).delete(any(SlotEntity.class));
    }

    @Test
    void deleteSlot_ManuallyBusySlot_IsAllowed() {
        when(slotRepository.findById(2L)).thenReturn(Optional.of(busySlotEntity));

        slotService.deleteSlot(2L);

        verify(slotRepository).delete(busySlotEntity);
    }

    @Test
    void deleteSlot_NotFound_ThrowsSlotNotFoundException() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SlotNotFoundException.class, () -> {
            slotService.deleteSlot(99L);
        });
    }
}