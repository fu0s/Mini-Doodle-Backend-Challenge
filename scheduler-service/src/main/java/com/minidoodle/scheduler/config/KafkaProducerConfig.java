package com.minidoodle.scheduler.config;

import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Explicit Kafka producer configuration for the scheduler-service.
 * Creates a {@link KafkaTemplate} parameterised on {@link MeetingCreatedEvent}
 * with {@link JsonSerializer} — no implicit defaults.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Creates a {@link ProducerFactory} with an explicit {@link JsonSerializer}
     * for meeting event payloads.
     */
    @Bean
    public ProducerFactory<String, MeetingCreatedEvent> meetingEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        bootstrapServers
                ),
                new StringSerializer(),
                new JsonSerializer<>()
        );
    }

    /**
     * Dedicated {@link KafkaTemplate} for sending {@link MeetingCreatedEvent}s.
     * The explicit serializer ensures type-safe JSON serialisation without
     * relying on Spring Boot auto-configured defaults.
     */
    @Bean
    public KafkaTemplate<String, MeetingCreatedEvent> meetingEventKafkaTemplate() {
        return new KafkaTemplate<>(meetingEventProducerFactory());
    }
}
