package com.minidoodle.consumer.listener;

import com.minidoodle.consumer.handler.MeetingCreatedEventHandler;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for {@link MeetingCreatedEvent}s.
 * <p>
 * Wires to the topic and group resolved from shared/consumer configuration via
 * SpEL bean references, using a dedicated
 * {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory}
 * with {@code JsonDeserializer}.
 * <p>
 * Delegates slot splitting to {@link MeetingCreatedEventHandler} which runs
 * asynchronously on a dedicated thread pool.
 */
@Component
public class MeetingCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MeetingCreatedEventConsumer.class);

    private final MeetingCreatedEventHandler eventHandler;

    public MeetingCreatedEventConsumer(MeetingCreatedEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * Receives a meeting-created event from Kafka and delegates to the handler.
     *
     * @param event the deserialised meeting-created event
     */
    @KafkaListener(
            topics = "#{sharedKafkaProperties.topicName}",
            groupId = "#{consumerProperties.groupId}",
            containerFactory = "meetingEventKafkaListenerContainerFactory"
    )
    public void onMeetingCreated(MeetingCreatedEvent event) {
        log.info("Received MeetingCreatedEvent [meetingId={}, participants={}, start={}, end={}]",
                event.getMeetingId(),
                event.getParticipants(),
                event.getStart(),
                event.getEnd());

        eventHandler.handleMeetingCreated(event);
    }
}
