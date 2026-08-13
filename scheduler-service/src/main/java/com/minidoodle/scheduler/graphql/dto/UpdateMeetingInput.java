package com.minidoodle.scheduler.graphql.dto;

import com.minidoodle.scheduler.graphql.validation.ValidTimeRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Input for the {@code updateMeeting} mutation. Matches
 * {@code UpdateMeetingInput} in the GraphQL schema.
 */
@ValidTimeRange
public record UpdateMeetingInput(
        @NotNull Long meetingId,
        @NotBlank String title,
        String description,
        @NotEmpty List<String> participants,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end) {
}