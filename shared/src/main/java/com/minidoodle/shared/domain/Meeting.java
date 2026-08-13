package com.minidoodle.shared.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing a meeting.
 * A meeting has a title, description, time range, and list of participant usernames.
 */
public class Meeting {
    
    private final Long id;
    private final String title;
    private final String description;
    private final List<String> participants;
    private final LocalDateTime start;
    private final LocalDateTime end;
    
    /**
     * Creates a new Meeting instance.
     *
     * @param id unique identifier
     * @param title meeting title
     * @param description meeting description
     * @param participants list of participant usernames
     * @param start meeting start time
     * @param end meeting end time
     */
    public Meeting(Long id, String title, String description, List<String> participants,
                   LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.participants = participants;
        this.start = start;
        this.end = end;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getParticipants() {
        return participants;
    }
    
    public LocalDateTime getStart() {
        return start;
    }
    
    public LocalDateTime getEnd() {
        return end;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meeting meeting = (Meeting) o;
        return Objects.equals(id, meeting.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Meeting{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", participants=" + participants +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
