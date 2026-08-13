package com.minidoodle.consumer.config;

import com.minidoodle.shared.constants.KafkaTopics;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Consumer-service-specific configuration.
 * <p>
 * Bound from the {@code scheduling.consumer.*} keys. Holds only what is
 * genuinely specific to the consumer service — cross-cutting Kafka values
 * (bootstrap servers, topic name, event type) live exclusively in the shared
 * {@link com.minidoodle.shared.config.SharedKafkaProperties} so producer and
 * consumer cannot drift apart.
 */
@ConfigurationProperties(prefix = "scheduling.consumer")
public class ConsumerProperties {

    /**
     * Explicit consumer group id for meeting-created events.
     */
    private String groupId = "consumer-service-meeting-created";

    /**
     * Number of consumer threads in the listener container.
     */
    private int concurrency = 3;

    /**
     * Dead-letter topic for meeting-created events that failed processing.
     */
    private String dltTopic = KafkaTopics.MEETING_CREATED_DLT;

    /**
     * Where a consumer with no committed offset starts reading.
     */
    private String autoOffsetReset = "earliest";

    /**
     * Back-off policy for failed event processing before dead-letter routing.
     */
    private Retry retry = new Retry();

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public String getDltTopic() {
        return dltTopic;
    }

    public void setDltTopic(String dltTopic) {
        this.dltTopic = dltTopic;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public void setAutoOffsetReset(String autoOffsetReset) {
        this.autoOffsetReset = autoOffsetReset;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    /**
     * {@code FixedBackOff} policy: how long to wait and how many times to retry
     * a failing event before it is routed to the dead-letter topic.
     */
    public static class Retry {

        /**
         * Interval (ms) between retry attempts.
         */
        private long intervalMs = 0;

        /**
         * Maximum number of retry attempts.
         */
        private int maxAttempts = 0;

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}