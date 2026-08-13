package com.minidoodle.consumer.handler;

import com.minidoodle.shared.event.MeetingCreatedEvent;
import com.minidoodle.shared.exceptions.MeetingEventProcessingException;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import com.minidoodle.shared.service.SlotSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MeetingCreatedEventHandlerTest {

    private SlotSplitter slotSplitter;
    private SlotRepository slotRepository;
    private MeetingCreatedEventHandler handler;

    @BeforeEach
    void setUp() {
        slotSplitter = mock(SlotSplitter.class);
        slotRepository = mock(SlotRepository.class);
        handler = new MeetingCreatedEventHandler(slotSplitter, slotRepository);
    }

    @Test
    void handleMeetingCreated_splitsSlotsForEachParticipant() {
        MeetingCreatedEvent event = new MeetingCreatedEvent(
                42L,
                List.of("alice", "bob", "charlie"),
                LocalDateTime.of(2026, 8, 15, 9, 0),
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        when(slotRepository.findByUsernameAndMeetingId(anyString(), eq(42L)))
                .thenReturn(Collections.emptyList());

        handler.handleMeetingCreated(event);

        verify(slotSplitter, times(3)).splitSlotsForBusyPeriod(
                anyString(), eq(event.getStart()), eq(event.getEnd()), eq(42L));

        ArgumentCaptor<String> participantCaptor = ArgumentCaptor.forClass(String.class);
        verify(slotSplitter, times(3)).splitSlotsForBusyPeriod(
                participantCaptor.capture(), any(), any(), anyLong());

        assertEquals(List.of("alice", "bob", "charlie"), participantCaptor.getAllValues());
    }

    @Test
    void handleMeetingCreated_skipsExistingParticipant() {
        MeetingCreatedEvent event = new MeetingCreatedEvent(
                1L,
                List.of("alice", "bob"),
                LocalDateTime.of(2026, 8, 15, 9, 0),
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        SlotEntity existingSlot = new SlotEntity();
        when(slotRepository.findByUsernameAndMeetingId("alice", 1L))
                .thenReturn(List.of(existingSlot));
        when(slotRepository.findByUsernameAndMeetingId("bob", 1L))
                .thenReturn(Collections.emptyList());

        handler.handleMeetingCreated(event);

        verify(slotSplitter, never()).splitSlotsForBusyPeriod(
                eq("alice"), any(), any(), anyLong());
        verify(slotSplitter).splitSlotsForBusyPeriod(
                eq("bob"), eq(event.getStart()), eq(event.getEnd()), eq(1L));
    }

    @Test
    void handleMeetingCreated_throwsOnParticipantFailure() {
        MeetingCreatedEvent event = new MeetingCreatedEvent(
                1L,
                List.of("alice", "bob"),
                LocalDateTime.of(2026, 8, 15, 9, 0),
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        when(slotRepository.findByUsernameAndMeetingId(anyString(), eq(1L)))
                .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("DB error"))
                .when(slotSplitter)
                .splitSlotsForBusyPeriod(eq("alice"), any(), any(), anyLong());

        MeetingEventProcessingException ex = assertThrows(
                MeetingEventProcessingException.class,
                () -> handler.handleMeetingCreated(event));

        assertEquals("MEETING_EVENT_PROCESSING", ex.getErrorCode().name());

        // bob should still be processed before the exception
        verify(slotSplitter).splitSlotsForBusyPeriod(
                eq("bob"), eq(event.getStart()), eq(event.getEnd()), eq(1L));
    }

    @Test
    void handleMeetingCreated_emptyParticipantList() {
        MeetingCreatedEvent event = new MeetingCreatedEvent(
                1L,
                List.of(),
                LocalDateTime.of(2026, 8, 15, 9, 0),
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        handler.handleMeetingCreated(event);

        verifyNoInteractions(slotSplitter);
    }
}
