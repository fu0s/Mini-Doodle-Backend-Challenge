package com.minidoodle.consumer.config;

import com.minidoodle.shared.config.SharedKafkaProperties;
import com.minidoodle.shared.event.MeetingCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Explicit Kafka consumer configuration for the consumer-service.
 * <p>
 * Every externalised value resolves from the shared
 * {@link SharedKafkaProperties} (bootstrap servers) or this service's
 * {@link ConsumerProperties} (group id, concurrency, DLT topic, retry
 * policy) — no {@code @Value} literals or raw placeholders in factories.
 */
@Configuration
public class KafkaConsumerConfig {

    private final SharedKafkaProperties sharedKafkaProperties;
    private final ConsumerProperties consumerProperties;

    public KafkaConsumerConfig(SharedKafkaProperties sharedKafkaProperties,
                               ConsumerProperties consumerProperties) {
        this.sharedKafkaProperties = sharedKafkaProperties;
        this.consumerProperties = consumerProperties;
    }

    /**
     * Creates a {@link ConsumerFactory} with an explicit {@link JsonDeserializer}
     * for {@link MeetingCreatedEvent} payloads.
     */
    @Bean
    public ConsumerFactory<String, MeetingCreatedEvent> meetingEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        sharedKafkaProperties.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, consumerProperties.getGroupId(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        consumerProperties.getAutoOffsetReset()
                ),
                new StringDeserializer(),
                new JsonDeserializer<>(MeetingCreatedEvent.class, false)
        );
    }

    /**
     * Producer factory for sending failed events to the dead-letter topic.
     */
    @Bean
    public ProducerFactory<String, MeetingCreatedEvent> dltProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        sharedKafkaProperties.getBootstrapServers()),
                new StringSerializer(),
                new JsonSerializer<>()
        );
    }

    /**
     * {@link KafkaTemplate} for publishing events to the dead-letter topic.
     */
    @Bean
    public KafkaTemplate<String, MeetingCreatedEvent> dltKafkaTemplate() {
        return new KafkaTemplate<>(dltProducerFactory());
    }

    /**
     * {@link DefaultErrorHandler} that forwards failed events to the
     * configured dead-letter topic with the configured back-off policy.
     * Uses {@link DeadLetterPublishingRecoverer} to publish to the DLT.
     */
    @Bean
    public DefaultErrorHandler meetingEventErrorHandler() {
        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver =
                (record, exception) -> new TopicPartition(
                        consumerProperties.getDltTopic(), record.partition());

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate(), destinationResolver);

        return new DefaultErrorHandler(recoverer, new FixedBackOff(
                consumerProperties.getRetry().getIntervalMs(),
                consumerProperties.getRetry().getMaxAttempts()));
    }

    /**
     * Dedicated {@link ConcurrentKafkaListenerContainerFactory} for meeting event
     * listeners. Uses the explicit consumer factory with {@link JsonDeserializer}
     * and a {@link DefaultErrorHandler} for dead-letter routing.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MeetingCreatedEvent>
            meetingEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MeetingCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(meetingEventConsumerFactory());
        factory.setConcurrency(consumerProperties.getConcurrency());
        factory.setCommonErrorHandler(meetingEventErrorHandler());
        return factory;
    }
}