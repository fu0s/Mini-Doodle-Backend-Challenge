package com.minidoodle.scheduler.kafka;

import com.minidoodle.shared.config.SharedKafkaProperties;
import com.minidoodle.shared.constants.KafkaTopics;
import com.minidoodle.shared.constants.MeetingEventType;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class KafkaMeetingEventPublisherTest {

    private KafkaTemplate<String, MeetingCreatedEvent> kafkaTemplate;
    private KafkaMeetingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new KafkaMeetingEventPublisher(kafkaTemplate, new SharedKafkaProperties());
    }

    @Test
    void publishMeetingCreated_sendsToCorrectTopic() {
        MeetingCreatedEvent event = new MeetingCreatedEvent(
                42L,
                List.of("alice", "bob"),
                LocalDateTime.of(2026, 8, 15, 9, 0),
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        publisher.publishMeetingCreated(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MeetingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(MeetingCreatedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals(KafkaTopics.MEETING_CREATED, topicCaptor.getValue());
        assertEquals(MeetingEventType.CREATED.name(), keyCaptor.getValue());
        assertEquals(42L, eventCaptor.getValue().getMeetingId());
        assertEquals(List.of("alice", "bob"), eventCaptor.getValue().getParticipants());
        assertEquals(event.getStart(), eventCaptor.getValue().getStart());
        assertEquals(event.getEnd(), eventCaptor.getValue().getEnd());
    }
}