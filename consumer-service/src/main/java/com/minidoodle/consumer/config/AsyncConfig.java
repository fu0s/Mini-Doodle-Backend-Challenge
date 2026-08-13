package com.minidoodle.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables {@code @Async} and provides a dedicated thread pool for the
 * consumer-service async slot-splitting tasks.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "slotSplittingExecutor")
    public Executor slotSplittingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("slot-split-");
        executor.initialize();
        return executor;
    }
}
