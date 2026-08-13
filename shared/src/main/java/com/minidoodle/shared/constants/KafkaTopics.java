package com.minidoodle.shared.constants;

/**
 * Centralized Kafka topic names.
 * No magic strings - all topic references go through this class.
 */
public final class KafkaTopics {
    
    /**
     * Topic for meeting lifecycle events (created, updated, deleted).
     */
    public static final String MEETING_EVENTS = "meeting-events";
    
    private KafkaTopics() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
