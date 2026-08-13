package com.minidoodle.scheduler.config;

import com.minidoodle.shared.domain.Meeting;
import com.minidoodle.shared.domain.Slot;
import org.dataloader.DataLoader;
import org.springframework.graphql.execution.DataLoaderRegistrar;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registers the graphql-java {@link DataLoader}s (see {@link DataLoaderConfig})
 * with the per-request {@link org.dataloader.DataLoaderRegistry} so resolvers can
 * batch nested lookups via {@code env.getDataLoader(name)}.
 */
@Component
public class SchedulerDataLoaderRegistrar implements DataLoaderRegistrar {

    private final DataLoader<Long, Meeting> meetingByIdLoader;
    private final DataLoader<String, List<Slot>> slotsByUsernameLoader;

    public SchedulerDataLoaderRegistrar(DataLoader<Long, Meeting> meetingByIdLoader,
                                        DataLoader<String, List<Slot>> slotsByUsernameLoader) {
        this.meetingByIdLoader = meetingByIdLoader;
        this.slotsByUsernameLoader = slotsByUsernameLoader;
    }

    @Override
    public void registerDataLoaders(org.dataloader.DataLoaderRegistry registry,
                                    graphql.GraphQLContext context) {
        registry.register("meetingById", meetingByIdLoader);
        registry.register("slotsByUsername", slotsByUsernameLoader);
    }
}