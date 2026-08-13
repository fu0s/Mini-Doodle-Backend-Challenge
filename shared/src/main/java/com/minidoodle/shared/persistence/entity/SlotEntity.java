package com.minidoodle.shared.persistence.entity;

import com.minidoodle.shared.constants.SlotStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing a time slot in the database.
 * Maps to the 'slots' table with indexes on (username, start, end) and meetingId.
 */
@Entity
@Table(name = "slots", indexes = {
    @Index(name = "idx_slots_username_time", columnList = "username, start_time, end_time"),
    @Index(name = "idx_slots_meeting_id", columnList = "meeting_id")
})
public class SlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotStatus status;

    @Column(name = "meeting_id", nullable = true)
    private Long meetingId;

    /**
     * Default constructor for JPA.
     */
    public SlotEntity() {
    }

    /**
     * Creates a new SlotEntity with the specified values.
     */
    public SlotEntity(Long id, String username, LocalDateTime startTime, LocalDateTime endTime,
                      SlotStatus status, Long meetingId) {
        this.id = id;
        this.username = username;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.meetingId = meetingId;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlotEntity that = (SlotEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SlotEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", meetingId=" + meetingId +
                '}';
    }
}