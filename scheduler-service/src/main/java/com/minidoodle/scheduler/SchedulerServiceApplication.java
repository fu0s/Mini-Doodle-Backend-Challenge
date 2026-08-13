package com.minidoodle.scheduler;

import com.minidoodle.shared.config.SharedKafkaProperties;
import com.minidoodle.scheduler.config.SchedulerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.minidoodle.scheduler", "com.minidoodle.shared"})
@EnableConfigurationProperties({SchedulerProperties.class, SharedKafkaProperties.class})
public class SchedulerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerServiceApplication.class, args);
    }
}
