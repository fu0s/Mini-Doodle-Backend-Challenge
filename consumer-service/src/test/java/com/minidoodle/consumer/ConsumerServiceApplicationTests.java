package com.minidoodle.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsumerServiceApplicationTests {

    @Test
    void smokeTest() {
        // Simple smoke test for Phase 1 - full integration tests will be added in later phases
        assertNotNull(ConsumerServiceApplication.class);
    }
}
