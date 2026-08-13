package com.minidoodle.shared.constants;

/**
 * Meeting event type enumeration.
 * Represents the lifecycle events for meetings published to Kafka.
 */
public enum MeetingEventType {
    /**
     * A new meeting has been created.
     */
    CREATED,
    
    /**
     * An existing meeting has been updated.
     */
    UPDATED,
    
    /**
     * A meeting has been deleted.
     */
    DELETED
}
