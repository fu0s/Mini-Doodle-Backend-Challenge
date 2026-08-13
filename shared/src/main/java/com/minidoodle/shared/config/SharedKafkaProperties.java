package com.minidoodle.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka configuration shared by both services.
 * <p>
 * Bound from the {@code scheduling.kafka.*} keys in {@code application.properties}.
 * The topic name and event type — which producer (scheduler-service) and
 * consumer (consumer-service) must agree on — resolve from this single shared
 * class, so a property change propagates to both services automatically.
 * No defaults live here; the values come exclusively from the properties file.
 */
@ConfigurationProperties(prefix = "scheduling.kafka")
@Component
public class SharedKafkaProperties {

    /**
     * Kafka bootstrap servers.
     */
    private String bootstrapServers;

    /**
     * Topic for meeting creation events.
     */
    private String topicName;

    /**
     * Type of meeting-created events, used as the Kafka message key.
     */
    private String eventType;

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