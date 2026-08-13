package com.minidoodle.consumer.listener;

import com.minidoodle.shared.constants.KafkaTopics;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the wiring of {@link MeetingCreatedEventConsumer}:
 * correct topic, consumer group, and container factory.
 */
class MeetingCreatedEventConsumerTest {

    @Test
    void consumerListenerHasCorrectTopic() throws NoSuchMethodException {
        Method method = MeetingCreatedEventConsumer.class.getMethod("onMeetingCreated", MeetingCreatedEvent.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);

        assertTrue(annotation != null, "onMeetingCreated must have @KafkaListener");
        assertEquals(1, annotation.topics().length);
        assertEquals(KafkaTopics.MEETING_CREATED, annotation.topics()[0]);
    }

    @Test
    void consumerListenerHasCorrectGroup() throws NoSuchMethodException {
        Method method = MeetingCreatedEventConsumer.class.getMethod("onMeetingCreated", MeetingCreatedEvent.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);

        assertTrue(annotation.groupId().contains("consumer-service-meeting-created"),
                "Consumer group must reference consumer-service-meeting-created");
    }

    @Test
    void consumerListenerUsesExplicitContainerFactory() throws NoSuchMethodException {
        Method method = MeetingCreatedEventConsumer.class.getMethod("onMeetingCreated", MeetingCreatedEvent.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);

        assertEquals("meetingEventKafkaListenerContainerFactory",
                annotation.containerFactory(),
                "Must use the explicit container factory with JsonDeserializer");
    }
}
