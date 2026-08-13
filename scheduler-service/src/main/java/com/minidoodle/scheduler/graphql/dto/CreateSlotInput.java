package com.minidoodle.scheduler.graphql.dto;

import com.minidoodle.scheduler.graphql.validation.ValidTimeRange;
import com.minidoodle.shared.constants.SlotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Input for the {@code createSlot} mutation. Matches {@code CreateSlotInput}
 * in the GraphQL schema; {@code status} defaults to {@code FREE}.
 */
@ValidTimeRange
public record CreateSlotInput(
        @NotBlank String username,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        SlotStatus status) {
}