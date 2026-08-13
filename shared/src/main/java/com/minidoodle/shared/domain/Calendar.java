package com.minidoodle.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate/view object representing a user's calendar.
 * Combines a username with their slots and meetings.
 * This is NOT a persisted entity - it's a view model for queries.
 */
public class Calendar {
    
    private final String username;
    private final List<Slot> slots;
    private final List<Meeting> meetings;
    
    /**
     * Creates a new Calendar view.
     *
     * @param username owner of this calendar
     * @param slots list of time slots for this user
     * @param meetings list of meetings involving this user
     */
    public Calendar(String username, List<Slot> slots, List<Meeting> meetings) {
        this.username = username;
        this.slots = slots;
        this.meetings = meetings;
    }
    
    public String getUsername() {
        return username;
    }
    
    public List<Slot> getSlots() {
        return slots;
    }
    
    public List<Meeting> getMeetings() {
        return meetings;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calendar calendar = (Calendar) o;
        return Objects.equals(username, calendar.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
    
    @Override
    public String toString() {
        return "Calendar{" +
                "username='" + username + '\'' +
                ", slots=" + slots.size() +
                ", meetings=" + meetings.size() +
                '}';
    }
}
