package com.minidoodle.shared.domain;

import com.minidoodle.shared.constants.SlotStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a time slot in a user's calendar.
 * A slot defines a period of time with a status (FREE or BUSY).
 * Busy slots may be linked to a meeting via meetingId.
 */
public class Slot {
    
    private final Long id;
    private final String username;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final SlotStatus status;
    private final Long meetingId; // nullable - set when this busy slot was created by a meeting
    
    /**
     * Creates a new Slot instance.
     *
     * @param id unique identifier
     * @param username owner of this slot
     * @param start start time of the slot
     * @param end end time of the slot
     * @param status availability status (FREE or BUSY)
     * @param meetingId optional meeting ID if this slot is linked to a meeting
     */
    public Slot(Long id, String username, LocalDateTime start, LocalDateTime end, 
                SlotStatus status, Long meetingId) {
        this.id = id;
        this.username = username;
        this.start = start;
        this.end = end;
        this.status = status;
        this.meetingId = meetingId;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public LocalDateTime getStart() {
        return start;
    }
    
    public LocalDateTime getEnd() {
        return end;
    }
    
    public SlotStatus getStatus() {
        return status;
    }
    
    public Long getMeetingId() {
        return meetingId;
    }
    
    /**
     * Checks if this slot is linked to a meeting.
     *
     * @return true if meetingId is not null
     */
    public boolean isLinkedToMeeting() {
        return meetingId != null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Slot slot = (Slot) o;
        return Objects.equals(id, slot.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Slot{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", status=" + status +
                ", meetingId=" + meetingId +
                '}';
    }
}
