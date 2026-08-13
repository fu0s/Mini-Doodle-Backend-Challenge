package com.minidoodle.scheduler.kafka;

import com.minidoodle.shared.config.SharedKafkaProperties;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import com.minidoodle.shared.service.MeetingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed implementation of {@link MeetingEventPublisher}.
 * Publishes {@link MeetingCreatedEvent}s to the topic resolved from
 * {@link SharedKafkaProperties} via an explicitly-configured
 * {@link KafkaTemplate} with {@code JsonSerializer}. The event type is used as
 * the Kafka message key.
 */
@Component
public class KafkaMeetingEventPublisher implements MeetingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMeetingEventPublisher.class);

    private final KafkaTemplate<String, MeetingCreatedEvent> kafkaTemplate;
    private final SharedKafkaProperties kafkaProperties;

    public KafkaMeetingEventPublisher(KafkaTemplate<String, MeetingCreatedEvent> kafkaTemplate,
                                      SharedKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * Publishes a meeting-created event to Kafka.
     *
     * @param event the meeting creation event
     */
    @Override
    public void publishMeetingCreated(MeetingCreatedEvent event) {
        log.info("Publishing MeetingCreatedEvent [meetingId={}] to topic '{}'",
                event.getMeetingId(), kafkaProperties.getTopicName());

        kafkaTemplate.send(kafkaProperties.getTopicName(), kafkaProperties.getEventType(), event);
    }
}
