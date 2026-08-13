package com.minidoodle.shared.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite ID class for MeetingParticipantEntity.
 */
public class MeetingParticipantId implements Serializable {

    private Long meeting;
    private String username;

    public MeetingParticipantId() {
    }

    public MeetingParticipantId(Long meeting, String username) {
        this.meeting = meeting;
        this.username = username;
    }

    public Long getMeeting() {
        return meeting;
    }

    public void setMeeting(Long meeting) {
        this.meeting = meeting;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingParticipantId that = (MeetingParticipantId) o;
        return Objects.equals(meeting, that.meeting) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meeting, username);
    }
}