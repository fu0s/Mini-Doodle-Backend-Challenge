package com.minidoodle.scheduler.graphql;

import com.minidoodle.scheduler.graphql.dto.CreateMeetingInput;
import com.minidoodle.scheduler.graphql.dto.UpdateMeetingInput;
import com.minidoodle.shared.domain.Meeting;
import com.minidoodle.shared.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL controller for meeting queries and mutations.
 * Calls the {@link MeetingService} interface only — never repositories.
 */
@Controller
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @QueryMapping
    public List<Meeting> meetingsByUsername(@Argument String username) {
        return meetingService.getMeetingsByUsername(username);
    }

    @MutationMapping
    public Meeting createMeeting(@Argument @Valid CreateMeetingInput input) {
        return meetingService.createMeeting(input.title(), input.description(), input.participants(),
                input.start(), input.end());
    }

    @MutationMapping
    public Meeting updateMeeting(@Argument @Valid UpdateMeetingInput input) {
        return meetingService.updateMeeting(input.meetingId(), input.title(), input.description(),
                input.participants(), input.start(), input.end());
    }

    @MutationMapping
    public boolean deleteMeeting(@Argument("meetingId") Long meetingId) {
        meetingService.deleteMeeting(meetingId);
        return true;
    }
}