package com.minidoodle.shared;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test-only Spring Boot configuration for repository integration tests.
 * The shared module is a library (no main application class), so these
 * annotations provide the configuration context {@code @DataJpaTest} needs.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.minidoodle.shared.persistence.entity")
@EnableJpaRepositories(basePackages = "com.minidoodle.shared.persistence.repository")
public class SharedTestApplication {
}