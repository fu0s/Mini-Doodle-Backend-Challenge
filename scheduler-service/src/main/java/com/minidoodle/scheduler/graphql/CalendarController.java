package com.minidoodle.scheduler.graphql;

import com.minidoodle.shared.domain.Calendar;
import com.minidoodle.shared.service.CalendarService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * GraphQL controller for the aggregated calendar view.
 * Calls the {@link CalendarService} interface only — never repositories.
 */
@Controller
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @QueryMapping
    public Calendar calendar(@Argument String username,
                             @Argument LocalDateTime from,
                             @Argument LocalDateTime to) {
        return calendarService.getCalendar(username, from, to);
    }
}