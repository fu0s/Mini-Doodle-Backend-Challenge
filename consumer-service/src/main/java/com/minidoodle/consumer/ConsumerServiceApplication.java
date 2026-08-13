package com.minidoodle.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.minidoodle.consumer", "com.minidoodle.shared"})
@EnableJpaRepositories(basePackages = "com.minidoodle.shared.persistence.repository")
@EntityScan(basePackages = "com.minidoodle.shared.persistence.entity")
public class ConsumerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerServiceApplication.class, args);
    }
}
