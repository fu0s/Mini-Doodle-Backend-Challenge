package com.minidoodle.scheduler.graphql.dto;

import com.minidoodle.scheduler.graphql.validation.ValidTimeRange;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Input for the {@code updateSlot} mutation. Matches {@code UpdateSlotInput}
 * in the GraphQL schema.
 */
@ValidTimeRange
public record UpdateSlotInput(
        @NotNull Long slotId,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end) {
}