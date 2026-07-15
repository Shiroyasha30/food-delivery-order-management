package com.fooddelivery.ordermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke IT: application context loads with security, JPA, and SQLite.
 * Full business flow coverage lives in {@link OrderLifecycleFlowIT}.
 */
@SpringBootTest
class DummyIT {

    @Test
    void contextLoads() {
        // context startup is the assertion
    }
}
