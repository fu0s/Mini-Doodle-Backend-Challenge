package com.minidoodle.shared.service.impl;

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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of MeetingService for meeting management operations.
 * Handles meeting creation, updates, deletion, and queries with proper validation.
 * Meeting operations automatically manage participant slots via SlotSplitter.
 */
@Service
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final SlotRepository slotRepository;
    private final SlotSplitter slotSplitter;
    private final MeetingMapper meetingMapper;
    private final ObjectProvider<MeetingEventPublisher> meetingEventPublisherProvider;

    public MeetingServiceImpl(MeetingRepository meetingRepository,
                              SlotRepository slotRepository,
                              SlotSplitter slotSplitter,
                              MeetingMapper meetingMapper,
                              ObjectProvider<MeetingEventPublisher> meetingEventPublisherProvider) {
        this.meetingRepository = meetingRepository;
        this.slotRepository = slotRepository;
        this.slotSplitter = slotSplitter;
        this.meetingMapper = meetingMapper;
        this.meetingEventPublisherProvider = meetingEventPublisherProvider;
    }

    @Override
    @Transactional
    public Meeting createMeeting(String title, String description, List<String> participants,
                                  LocalDateTime start, LocalDateTime end) {
        validateMeetingInput(title, participants, start, end);
        verifyParticipantsAvailability(participants, start, end);

        MeetingEntity meetingEntity = new MeetingEntity();
        meetingEntity.setTitle(title);
        meetingEntity.setDescription(description);
        meetingEntity.setStartTime(start);
        meetingEntity.setEndTime(end);

        for (String participant : participants) {
            meetingEntity.addParticipant(participant);
        }

        MeetingEntity saved = meetingRepository.save(meetingEntity);

        MeetingEventPublisher publisher = meetingEventPublisherProvider.getIfAvailable();
        if (publisher != null) {
            publisher.publishMeetingCreated(
                    new MeetingCreatedEvent(saved.getId(), participants, start, end));
        }

        return meetingMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public Meeting updateMeeting(Long meetingId, String title, String description,
                                  List<String> participants, LocalDateTime start, LocalDateTime end) {
        validateMeetingInput(title, participants, start, end);

        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        slotSplitter.freeSlotsForMeeting(meetingId);
        verifyParticipantsAvailability(participants, start, end);

        meeting.setTitle(title);
        meeting.setDescription(description);
        meeting.setStartTime(start);
        meeting.setEndTime(end);
        meeting.getParticipants().clear();
        for (String participant : participants) {
            meeting.addParticipant(participant);
        }

        MeetingEntity saved = meetingRepository.save(meeting);

        for (String participant : participants) {
            slotSplitter.splitSlotsForBusyPeriod(participant, start, end, saved.getId());
        }

        return meetingMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteMeeting(Long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        slotSplitter.freeSlotsForMeeting(meetingId);
        meetingRepository.delete(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByUsername(String username) {
        List<MeetingEntity> entities = meetingRepository.findByParticipantUsername(username);
        return meetingMapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Meeting getMeetingById(Long meetingId) {
        MeetingEntity entity = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        return meetingMapper.toDomain(entity);
    }

    private void validateMeetingInput(String title, List<String> participants,
                                       LocalDateTime start, LocalDateTime end) {
        if (end == null || start == null || !end.isAfter(start)) {
            throw new InvalidTimeRangeException(start, end);
        }

        if (title == null || title.trim().isEmpty()) {
            throw new InvalidParticipantsException("Meeting title cannot be empty");
        }

        if (participants == null || participants.isEmpty()) {
            throw new InvalidParticipantsException("Participants list cannot be empty");
        }

        for (String participant : participants) {
            if (participant == null || participant.trim().isEmpty()) {
                throw new InvalidParticipantsException("Participant usernames cannot be null or empty");
            }
        }
    }

    private void verifyParticipantsAvailability(List<String> participants,
                                                  LocalDateTime start, LocalDateTime end) {
        List<String> unavailableParticipants = new ArrayList<>();

        for (String participant : participants) {
            List<SlotEntity> freeSlots = slotRepository.findFreeSlotsCoveringRange(participant, start, end);
            if (freeSlots.isEmpty()) {
                unavailableParticipants.add(participant);
            }
        }

        if (!unavailableParticipants.isEmpty()) {
            throw new SlotConflictException(
                    String.format("Participants not available: %s", unavailableParticipants));
        }
    }
}
