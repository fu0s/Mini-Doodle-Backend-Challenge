package com.minidoodle.shared.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a meeting is created.
 * Carries the data required for async slot splitting in the consumer service.
 */
public class MeetingCreatedEvent {

    private Long meetingId;
    private List<String> participants;
    private LocalDateTime start;
    private LocalDateTime end;

    public MeetingCreatedEvent() {
    }

    public MeetingCreatedEvent(Long meetingId, List<String> participants,
                               LocalDateTime start, LocalDateTime end) {
        this.meetingId = meetingId;
        this.participants = participants;
        this.start = start;
        this.end = end;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }
}
