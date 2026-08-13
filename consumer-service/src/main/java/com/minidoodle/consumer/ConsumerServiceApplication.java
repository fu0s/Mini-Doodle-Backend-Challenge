package com.minidoodle.consumer;

import com.minidoodle.shared.config.SharedKafkaProperties;
import com.minidoodle.consumer.config.ConsumerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.minidoodle.consumer", "com.minidoodle.shared"})
@EnableConfigurationProperties({ConsumerProperties.class, SharedKafkaProperties.class})
public class ConsumerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerServiceApplication.class, args);
    }
}
