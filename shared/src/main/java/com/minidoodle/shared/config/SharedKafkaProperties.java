package com.minidoodle.shared.config;

import com.minidoodle.shared.constants.KafkaTopics;
import com.minidoodle.shared.constants.MeetingEventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka configuration shared by both services.
 * <p>
 * Bound from the {@code scheduling.kafka.*} keys so that the topic name and
 * event type — which producer (scheduler-service) and consumer (consumer-service)
 * must agree on — resolve from this single shared class. A property change
 * therefore propagates to both services automatically.
 */
@ConfigurationProperties(prefix = "scheduling.kafka")
public class SharedKafkaProperties {

    /**
     * Kafka bootstrap servers.
     */
    private String bootstrapServers = "localhost:9092";

    /**
     * Topic for meeting creation events.
     */
    private String topicName = KafkaTopics.MEETING_CREATED;

    /**
     * Type of meeting-created events, used as the Kafka message key.
     */
    private String eventType = MeetingEventType.CREATED.name();

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}