package com.minidoodle.shared.service;

import com.minidoodle.shared.domain.Calendar;

import java.time.LocalDateTime;

/**
 * Service interface for calendar view operations.
 * Provides aggregate views combining slots and meetings for a user.
 */
public interface CalendarService {
    
    /**
     * Retrieves a calendar view for a user, optionally filtered by time range.
     * If from and to are null, returns all slots and meetings.
     *
     * @param username the username to query
     * @param from optional start of time range filter (null for no lower bound)
     * @param to optional end of time range filter (null for no upper bound)
     * @return calendar view containing slots and meetings
     */
    Calendar getCalendar(String username, LocalDateTime from, LocalDateTime to);
}
