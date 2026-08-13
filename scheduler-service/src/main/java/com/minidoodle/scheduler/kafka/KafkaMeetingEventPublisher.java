package com.minidoodle.scheduler.kafka;

import com.minidoodle.shared.constants.KafkaTopics;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import com.minidoodle.shared.service.MeetingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed implementation of {@link MeetingEventPublisher}.
 * Publishes {@link MeetingCreatedEvent}s to the {@code meeting-created} topic
 * via an explicitly-configured {@link KafkaTemplate} with {@code JsonSerializer}.
 */
@Component
public class KafkaMeetingEventPublisher implements MeetingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMeetingEventPublisher.class);

    private final KafkaTemplate<String, MeetingCreatedEvent> kafkaTemplate;

    public KafkaMeetingEventPublisher(KafkaTemplate<String, MeetingCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a meeting-created event to Kafka.
     *
     * @param event the meeting creation event
     */
    @Override
    public void publishMeetingCreated(MeetingCreatedEvent event) {
        log.info("Publishing MeetingCreatedEvent [meetingId={}] to topic '{}'",
                event.getMeetingId(), KafkaTopics.MEETING_CREATED);

        kafkaTemplate.send(KafkaTopics.MEETING_CREATED, event);
    }
}
