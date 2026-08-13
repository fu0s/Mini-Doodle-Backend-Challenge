package com.minidoodle.consumer.listener;

import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the wiring of {@link MeetingCreatedEventConsumer}:
 * shared-topic SpEL reference, consumer group, and container factory.
 */
class MeetingCreatedEventConsumerTest {

    @Test
    void consumerListenerHasCorrectTopic() throws NoSuchMethodException {
        Method method = MeetingCreatedEventConsumer.class.getMethod("onMeetingCreated", MeetingCreatedEvent.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);

        assertTrue(annotation != null, "onMeetingCreated must have @KafkaListener");
        assertEquals(1, annotation.topics().length);
        assertEquals("#{sharedKafkaProperties.topicName}", annotation.topics()[0]);
    }

    @Test
    void consumerListenerHasCorrectGroup() throws NoSuchMethodException {
        Method method = MeetingCreatedEventConsumer.class.getMethod("onMeetingCreated", MeetingCreatedEvent.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);

        assertEquals("#{consumerProperties.groupId}", annotation.groupId(),
                "Consumer group must resolve from ConsumerProperties");
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