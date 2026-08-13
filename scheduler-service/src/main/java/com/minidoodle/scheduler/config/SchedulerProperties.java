package com.minidoodle.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Scheduler-service-specific configuration.
 * <p>
 * Bound from the {@code scheduling.scheduler.*} keys in
 * {@code application.properties}. Holds only what is genuinely specific to the
 * scheduler service — cross-cutting Kafka values (bootstrap servers, topic name,
 * event type) live exclusively in the shared
 * {@link com.minidoodle.shared.config.SharedKafkaProperties} so producer and
 * consumer cannot drift apart. No defaults live here; the values come
 * exclusively from the properties file.
 */
@ConfigurationProperties(prefix = "scheduling.scheduler")
@Component
public class SchedulerProperties {

    /**
     * GraphQL HTTP endpoint path served by this service.
     */
    private String graphqlPath;

    /**
     * Kafka producer tuning for meeting events.
     */
    private Producer producer;

    public String getGraphqlPath() {
        return graphqlPath;
    }

    public void setGraphqlPath(String graphqlPath) {
        this.graphqlPath = graphqlPath;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    /**
     * Producer-side tunables applied to the meeting-event {@code KafkaTemplate}.
     */
    public static class Producer {

        /**
         * Required acknowledgements for meeting events.
         */
        private String acks;

        /**
         * Number of retries for a failed send.
         */
        private int retries;

        /**
         * Accumulator linger (ms) before a batch is sent.
         */
        private long lingerMs;

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

        public long getLingerMs() {
            return lingerMs;
        }

        public void setLingerMs(long lingerMs) {
            this.lingerMs = lingerMs;
        }
    }
}