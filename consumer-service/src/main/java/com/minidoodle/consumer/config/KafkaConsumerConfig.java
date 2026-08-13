package com.minidoodle.consumer.config;

import com.minidoodle.shared.constants.KafkaTopics;
import com.minidoodle.shared.event.MeetingCreatedEvent;
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
 * Creates a {@link ConcurrentKafkaListenerContainerFactory} with
 * {@link JsonDeserializer} and a {@link DefaultErrorHandler} that
 * forwards failed events to the dead-letter topic.
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Creates a {@link ConsumerFactory} with an explicit {@link JsonDeserializer}
     * for {@link MeetingCreatedEvent} payloads.
     */
    @Bean
    public ConsumerFactory<String, MeetingCreatedEvent> meetingEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        "bootstrap.servers", "${spring.kafka.bootstrap-servers:localhost:9092}",
                        "group.id", "${spring.kafka.consumer.group-id:consumer-service-meeting-created}",
                        "auto.offset.reset", "earliest"
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
                Map.of("bootstrap.servers", "${spring.kafka.bootstrap-servers:localhost:9092}"),
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
     * {@code meeting-created.DLT} topic with no retry (fixed back-off of 0).
     * Uses {@link DeadLetterPublishingRecoverer} to publish to the DLT.
     */
    @Bean
    public DefaultErrorHandler meetingEventErrorHandler() {
        BiFunction<org.apache.kafka.clients.consumer.ConsumerRecord<?,?>, Exception,
                org.apache.kafka.common.TopicPartition> destinationResolver =
                (record, exception) -> new org.apache.kafka.common.TopicPartition(
                        KafkaTopics.MEETING_CREATED_DLT, record.partition());

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate(), destinationResolver);

        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
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
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(meetingEventErrorHandler());
        return factory;
    }
}
