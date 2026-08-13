package com.minidoodle.shared.service.impl;

import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.domain.Meeting;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import com.minidoodle.shared.exceptions.InvalidParticipantsException;
import com.minidoodle.shared.exceptions.InvalidTimeRangeException;
import com.minidoodle.shared.exceptions.MeetingNotFoundException;
import com.minidoodle.shared.exceptions.SlotConflictException;
import com.minidoodle.shared.mapper.MeetingMapper;
import com.minidoodle.shared.persistence.entity.MeetingEntity;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.MeetingRepository;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import com.minidoodle.shared.service.MeetingEventPublisher;
import com.minidoodle.shared.service.MeetingService;
import com.minidoodle.shared.service.SlotSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MeetingServiceImpl}.
 * Covers the availability-check logic (verifyParticipantsAvailability) and the
 * create/update/delete/query flows that depend on it.
 */
@ExtendWith(MockitoExtension.class)
class MeetingServiceImplTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private SlotSplitter slotSplitter;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock
    private ObjectProvider<MeetingEventPublisher> meetingEventPublisherProvider;

    @Mock
    private MeetingEventPublisher meetingEventPublisher;

    private MeetingService meetingService;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        meetingService = new MeetingServiceImpl(
                meetingRepository, slotRepository, slotSplitter,
                meetingMapper, meetingEventPublisherProvider);
        start = LocalDateTime.of(2026, 1, 1, 9, 0);
        end = start.plusHours(1);
    }

    @Test
    void createMeeting_allParticipantsAvailable_savesMeetingAndPublishesEvent() {
        when(meetingEventPublisherProvider.getIfAvailable()).thenReturn(meetingEventPublisher);
        SlotEntity freeSlot = new SlotEntity(1L, "alice", start, end, SlotStatus.FREE, null);
        when(slotRepository.findFreeSlotsCoveringRange(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(freeSlot));
        when(meetingRepository.save(any(MeetingEntity.class))).thenAnswer(inv -> {
            MeetingEntity e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });
        Meeting expected = new Meeting(100L, "Sync", "desc", List.of("alice"), start, end);
        when(meetingMapper.toDomain(any(MeetingEntity.class))).thenReturn(expected);

        Meeting result = meetingService.createMeeting("Sync", "desc", List.of("alice"), start, end);

        assertEquals(expected, result);

        ArgumentCaptor<MeetingEntity> entityCaptor = ArgumentCaptor.forClass(MeetingEntity.class);
        verify(meetingRepository).save(entityCaptor.capture());
        assertEquals("Sync", entityCaptor.getValue().getTitle());
        assertEquals(1, entityCaptor.getValue().getParticipants().size());

        ArgumentCaptor<MeetingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(MeetingCreatedEvent.class);
        verify(meetingEventPublisher).publishMeetingCreated(eventCaptor.capture());
        assertEquals(100L, eventCaptor.getValue().getMeetingId());
        assertEquals(List.of("alice"), eventCaptor.getValue().getParticipants());
        assertEquals(start, eventCaptor.getValue().getStart());
        assertEquals(end, eventCaptor.getValue().getEnd());
    }

    @Test
    void createMeeting_participantWithoutFreeSlot_throwsSlotConflictAndDoesNotSave() {
        when(slotRepository.findFreeSlotsCoveringRange("alice", start, end)).thenReturn(List.of());

        SlotConflictException ex = assertThrows(SlotConflictException.class,
                () -> meetingService.createMeeting("Sync", "desc", List.of("alice"), start, end));

        assertTrue(ex.getMessage().contains("alice"));
        verify(meetingRepository, never()).save(any(MeetingEntity.class));
        verify(meetingEventPublisherProvider, never()).getIfAvailable();
    }

    @Test
    void createMeeting_oneOfMultipleUnavailable_throwsSlotConflict() {
        SlotEntity freeSlot = new SlotEntity(1L, "alice", start, end, SlotStatus.FREE, null);
        when(slotRepository.findFreeSlotsCoveringRange("alice", start, end)).thenReturn(List.of(freeSlot));
        when(slotRepository.findFreeSlotsCoveringRange("bob", start, end)).thenReturn(List.of());

        SlotConflictException ex = assertThrows(SlotConflictException.class,
                () -> meetingService.createMeeting("Sync", "desc", List.of("alice", "bob"), start, end));

        assertTrue(ex.getMessage().contains("bob"));
        verify(meetingRepository, never()).save(any(MeetingEntity.class));
    }

    @Test
    void createMeeting_invalidTimeRange_throws() {
        assertThrows(InvalidTimeRangeException.class,
                () -> meetingService.createMeeting("Sync", "desc", List.of("alice"), end, start));
    }

    @Test
    void createMeeting_emptyTitle_throws() {
        assertThrows(InvalidParticipantsException.class,
                () -> meetingService.createMeeting("  ", "desc", List.of("alice"), start, end));
    }

    @Test
    void createMeeting_emptyParticipants_throws() {
        assertThrows(InvalidParticipantsException.class,
                () -> meetingService.createMeeting("Sync", "desc", List.of(), start, end));
    }

    @Test
    void createMeeting_blankParticipant_throws() {
        assertThrows(InvalidParticipantsException.class,
                () -> meetingService.createMeeting("Sync", "desc", List.of("alice", " "), start, end));
    }

    @Test
    void createMeeting_noPublisherBean_skipsPublish() {
        when(slotRepository.findFreeSlotsCoveringRange(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new SlotEntity(1L, "alice", start, end, SlotStatus.FREE, null)));
        when(meetingRepository.save(any(MeetingEntity.class))).thenAnswer(inv -> {
            MeetingEntity e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });
        when(meetingMapper.toDomain(any(MeetingEntity.class)))
                .thenReturn(new Meeting(100L, "Sync", "desc", List.of("alice"), start, end));

        meetingService.createMeeting("Sync", "desc", List.of("alice"), start, end);

        verify(meetingEventPublisher, never()).publishMeetingCreated(any(MeetingCreatedEvent.class));
    }

    @Test
    void updateMeeting_meetingNotFound_throws() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class,
                () -> meetingService.updateMeeting(100L, "New", "desc", List.of("alice"), start, end));
    }

    @Test
    void deleteMeeting_meetingNotFound_throws() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class,
                () -> meetingService.deleteMeeting(100L));
    }

    @Test
    void updateMeeting_availableParticipants_freesAndReSplitsSlots() {
        MeetingEntity existing = new MeetingEntity(100L, "Old", "desc", start, end);
        existing.addParticipant("alice");
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(existing));
        SlotEntity freeSlot = new SlotEntity(1L, "alice", start, end, SlotStatus.FREE, null);
        when(slotRepository.findFreeSlotsCoveringRange(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(freeSlot));
        when(meetingRepository.save(any(MeetingEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMapper.toDomain(any(MeetingEntity.class)))
                .thenReturn(new Meeting(100L, "New", "desc", List.of("alice", "carl"), start, end));

        Meeting result = meetingService.updateMeeting(100L, "New", "desc", List.of("alice", "carl"), start, end);

        assertEquals("New", result.getTitle());
        verify(slotSplitter).freeSlotsForMeeting(100L);
        verify(slotSplitter).splitSlotsForBusyPeriod("alice", start, end, 100L);
        verify(slotSplitter).splitSlotsForBusyPeriod("carl", start, end, 100L);
        assertEquals(2, existing.getParticipants().size());
    }

    @Test
    void getMeetingById_notFound_throws() {
        when(meetingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> meetingService.getMeetingById(99L));
    }

    @Test
    void getMeetingsByUsername_delegatesToMapper() {
        MeetingEntity entity = new MeetingEntity(1L, "Sync", "desc", start, end);
        when(meetingRepository.findByParticipantUsername("alice")).thenReturn(List.of(entity));
        when(meetingMapper.toDomainList(anyList()))
                .thenReturn(List.of(new Meeting(1L, "Sync", "desc", List.of("alice"), start, end)));

        List<Meeting> result = meetingService.getMeetingsByUsername("alice");

        assertEquals(1, result.size());
        assertEquals("Sync", result.get(0).getTitle());
    }

    @Test
    void getMeetingsByIds_delegatesToMapper() {
        when(meetingRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of());
        when(meetingMapper.toDomainList(List.of())).thenReturn(List.of());

        List<Meeting> result = meetingService.getMeetingsByIds(List.of(1L, 2L));

        assertTrue(result.isEmpty());
    }
}