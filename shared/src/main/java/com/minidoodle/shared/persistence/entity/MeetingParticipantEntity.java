package com.minidoodle.shared.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * JPA entity representing a meeting participant in the database.
 * Maps to the 'meeting_participants' table.
 * This is a join table entity to support the many-to-many relationship between meetings and users.
 */
@Entity
@Table(name = "meeting_participants", indexes = {
    @Index(name = "idx_meeting_participants_meeting", columnList = "meeting_id"),
    @Index(name = "idx_meeting_participants_username", columnList = "username")
})
@IdClass(MeetingParticipantId.class)
public class MeetingParticipantEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingEntity meeting;

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    /**
     * Default constructor for JPA.
     */
    public MeetingParticipantEntity() {
    }

    /**
     * Creates a new MeetingParticipantEntity.
     */
    public MeetingParticipantEntity(MeetingEntity meeting, String username) {
        this.meeting = meeting;
        this.username = username;
    }

    // Getters and Setters

    public MeetingEntity getMeeting() {
        return meeting;
    }

    public void setMeeting(MeetingEntity meeting) {
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
        MeetingParticipantEntity that = (MeetingParticipantEntity) o;
        return Objects.equals(meeting != null ? meeting.getId() : null, that.meeting != null ? that.meeting.getId() : null) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meeting != null ? meeting.getId() : null, username);
    }

    @Override
    public String toString() {
        return "MeetingParticipantEntity{" +
                "meetingId=" + (meeting != null ? meeting.getId() : null) +
                ", username='" + username + '\'' +
                '}';
    }
}