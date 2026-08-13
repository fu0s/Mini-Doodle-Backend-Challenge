package com.minidoodle.scheduler.config;

import com.minidoodle.shared.domain.Meeting;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.service.MeetingService;
import com.minidoodle.shared.service.SlotService;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.MappedBatchLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GraphQL DataLoader configuration.
 * {@link MappedBatchLoader}s delegate to service interfaces (never repositories)
 * and batch otherwise-equivalent N+1 queries into a single call.
 */
@Configuration
public class DataLoaderConfig {

    /**
     * Batched resolution of meetings by ID.
     * Resolves many {@code meetingId -> Meeting} lookups (e.g. nested slot
     * resolution) with a single repository call.
     *
     * @param meetingService the meeting service interface
     * @return a DataLoader keyed by meeting ID
     */
    @Bean
    public DataLoader<Long, Meeting> meetingByIdLoader(MeetingService meetingService) {
        MappedBatchLoader<Long, Meeting> batchLoader = meetingIds ->
                CompletableFuture.supplyAsync(() -> meetingsById(meetingService.getMeetingsByIds(meetingIds)));
        return DataLoaderFactory.newMappedDataLoader(batchLoader);
    }

    /**
     * Batched resolution of slots by owner username.
     * Resolves many {@code username -> slots} lookups with a single repository call.
     *
     * @param slotService the slot service interface
     * @return a DataLoader keyed by username
     */
    @Bean
    public DataLoader<String, List<Slot>> slotsByUsernameLoader(SlotService slotService) {
        MappedBatchLoader<String, List<Slot>> batchLoader = usernames ->
                CompletableFuture.supplyAsync(() -> slotsByUsername(slotService.getSlotsByUsernames(usernames)));
        return DataLoaderFactory.newMappedDataLoader(batchLoader);
    }

    private static Map<Long, Meeting> meetingsById(Collection<Meeting> meetings) {
        return meetings.stream()
                .collect(Collectors.toMap(Meeting::getId, Function.identity()));
    }

    private static Map<String, List<Slot>> slotsByUsername(Collection<Slot> slots) {
        return slots.stream()
                .collect(Collectors.groupingBy(Slot::getUsername));
    }
}