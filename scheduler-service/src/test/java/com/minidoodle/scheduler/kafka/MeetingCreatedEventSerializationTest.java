package com.minidoodle.scheduler.kafka;

import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ensures {@link MeetingCreatedEvent} survives a JSON round-trip using the same
 * serializer/deserializer pair as the production Kafka configuration
 * (see {@code KafkaProducerConfig} and {@code KafkaConsumerConfig}).
 * Critical because the event carries {@link LocalDateTime} fields.
 */
class MeetingCreatedEventSerializationTest {

    @Test
    void meetingCreatedEvent_roundTripsThroughJsonSerializerDeserializer() {
        MeetingCreatedEvent original = new MeetingCreatedEvent(
                42L,
                List.of("alice", "bob"),
                LocalDateTime.of(2026, 8, 15, 9, 0),
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        JsonSerializer<MeetingCreatedEvent> serializer = new JsonSerializer<>();
        byte[] json = serializer.serialize("meeting-created", original);

        JsonDeserializer<MeetingCreatedEvent> deserializer =
                new JsonDeserializer<>(MeetingCreatedEvent.class, false);
        MeetingCreatedEvent restored =
                deserializer.deserialize("meeting-created", json);

        assertEquals(42L, restored.getMeetingId());
        assertEquals(List.of("alice", "bob"), restored.getParticipants());
        assertEquals(LocalDateTime.of(2026, 8, 15, 9, 0), restored.getStart());
        assertEquals(LocalDateTime.of(2026, 8, 15, 10, 0), restored.getEnd());
    }

    @Test
    void meetingCreatedEvent_withoutTimeFields_stillDeserializes() {
        byte[] json = "{\"meetingId\":7,\"participants\":[\"zoe\"]}".getBytes();

        JsonDeserializer<MeetingCreatedEvent> deserializer =
                new JsonDeserializer<>(MeetingCreatedEvent.class, false);
        MeetingCreatedEvent restored =
                deserializer.deserialize("meeting-created", json);

        assertEquals(7L, restored.getMeetingId());
        assertEquals(List.of("zoe"), restored.getParticipants());
        assertEquals(null, restored.getStart());
        assertEquals(null, restored.getEnd());
    }
}