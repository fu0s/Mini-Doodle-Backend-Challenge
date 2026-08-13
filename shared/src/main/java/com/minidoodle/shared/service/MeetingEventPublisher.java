package com.minidoodle.shared.service;

import com.minidoodle.shared.event.MeetingCreatedEvent;

/**
 * Publishes meeting lifecycle events to the message bus.
 * Implementations are provided by the messaging layer of each service
 * that publishes events; consumers without a publisher may omit the bean.
 */
public interface MeetingEventPublisher {

    /**
     * Publishes an event indicating that a meeting has been created.
     *
     * @param event the meeting creation event
     */
    void publishMeetingCreated(MeetingCreatedEvent event);
}
