package com.minidoodle.scheduler.config;

import com.minidoodle.shared.config.SharedKafkaProperties;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Explicit Kafka producer configuration for the scheduler-service.
 * <p>
 * Every externalised value resolves from the shared
 * {@link SharedKafkaProperties} (bootstrap servers) or this service's
 * {@link SchedulerProperties} (producer tuning) — no {@code @Value} literals.
 */
@Configuration
public class KafkaProducerConfig {

    private final SharedKafkaProperties sharedKafkaProperties;
    private final SchedulerProperties schedulerProperties;

    public KafkaProducerConfig(SharedKafkaProperties sharedKafkaProperties,
                               SchedulerProperties schedulerProperties) {
        this.sharedKafkaProperties = sharedKafkaProperties;
        this.schedulerProperties = schedulerProperties;
    }

    /**
     * Creates a {@link ProducerFactory} with an explicit {@link JsonSerializer}
     * for meeting event payloads, tuned via {@link SchedulerProperties.Producer}.
     */
    @Bean
    public ProducerFactory<String, MeetingCreatedEvent> meetingEventProducerFactory() {
        SchedulerProperties.Producer producer = schedulerProperties.getProducer();
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        sharedKafkaProperties.getBootstrapServers(),
                        ProducerConfig.ACKS_CONFIG, producer.getAcks(),
                        ProducerConfig.RETRIES_CONFIG, producer.getRetries(),
                        ProducerConfig.LINGER_MS_CONFIG, producer.getLingerMs()
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