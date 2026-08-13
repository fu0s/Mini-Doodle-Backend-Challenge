package com.minidoodle.consumer.handler;

import com.minidoodle.shared.event.MeetingCreatedEvent;
import com.minidoodle.shared.exceptions.MeetingEventProcessingException;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import com.minidoodle.shared.service.SlotSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Asynchronous handler for {@link MeetingCreatedEvent}s.
 * Runs {@link SlotSplitter#splitSlotsForBusyPeriod} for each participant
 * on a dedicated thread pool, tagging BUSY slots with the event's meetingId.
 * <p>
 * Skips participants whose BUSY slots already exist for the given meetingId
 * (idempotency for redelivery safety).
 * <p>
 * Wraps per-participant failures in {@link MeetingEventProcessingException}
 * and propagates to trigger dead-letter routing. Never silently drops.
 */
@Component
public class MeetingCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(MeetingCreatedEventHandler.class);

    private final SlotSplitter slotSplitter;
    private final SlotRepository slotRepository;

    public MeetingCreatedEventHandler(SlotSplitter slotSplitter, SlotRepository slotRepository) {
        this.slotSplitter = slotSplitter;
        this.slotRepository = slotRepository;
    }

    /**
     * Asynchronously splits FREE slots for every participant in the event,
     * creating BUSY slots tagged with the meetingId.
     * <p>
     * If a slot already exists for the participant with the given meetingId,
     * splitting is skipped (idempotent on redelivery).
     * <p>
     * If any participant fails, all failures are collected and wrapped in
     * {@link MeetingEventProcessingException} to trigger dead-letter routing.
     *
     * @param event the meeting-created event carrying participants and time range
     * @throws MeetingEventProcessingException if one or more participants failed
     */
    @Async("slotSplittingExecutor")
    public void handleMeetingCreated(MeetingCreatedEvent event) {
        log.info("Processing MeetingCreatedEvent [meetingId={}] for {} participants",
                event.getMeetingId(), event.getParticipants().size());

        List<String> failedParticipants = new ArrayList<>();

        for (String participant : event.getParticipants()) {
            try {
                List<SlotEntity> existing = slotRepository.findByUsernameAndMeetingId(
                        participant, event.getMeetingId());
                if (!existing.isEmpty()) {
                    log.info("Skipping participant '{}' [meetingId={}]: slots already exist",
                            participant, event.getMeetingId());
                    continue;
                }

                slotSplitter.splitSlotsForBusyPeriod(
                        participant,
                        event.getStart(),
                        event.getEnd(),
                        event.getMeetingId()
                );
                log.debug("Slot splitting completed for participant '{}' [meetingId={}]",
                        participant, event.getMeetingId());
            } catch (Exception e) {
                log.error("Failed to split slots for participant '{}' [meetingId={}]",
                        participant, event.getMeetingId(), e);
                failedParticipants.add(participant);
            }
        }

        if (!failedParticipants.isEmpty()) {
            throw new MeetingEventProcessingException(
                    String.format("Failed to process meeting event [meetingId=%d] for participants: %s",
                            event.getMeetingId(), failedParticipants),
                    null);
        }

        log.info("MeetingCreatedEvent [meetingId={}] processing complete",
                event.getMeetingId());
    }
}
