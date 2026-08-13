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

    /**
     * Topic for meeting creation events (async slot splitting).
     */
    public static final String MEETING_CREATED = "meeting-created";

    /**
     * Dead letter topic for meeting creation events that failed processing.
     */
    public static final String MEETING_CREATED_DLT = "meeting-created.DLT";
    
    private KafkaTopics() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
