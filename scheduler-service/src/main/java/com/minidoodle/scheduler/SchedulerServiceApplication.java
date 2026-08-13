package com.minidoodle.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.minidoodle.scheduler", "com.minidoodle.shared"})
@EnableJpaRepositories(basePackages = "com.minidoodle.shared.persistence.repository")
@EntityScan(basePackages = "com.minidoodle.shared.persistence.entity")
public class SchedulerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerServiceApplication.class, args);
    }
}
