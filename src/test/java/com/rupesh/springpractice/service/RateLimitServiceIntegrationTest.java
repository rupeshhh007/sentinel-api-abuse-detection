package com.rupesh.springpractice.service;

import com.rupesh.springpractice.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RateLimitService rateLimitService;

    @Test
    void shouldAllowRequestsUnderLimit() {
        String testFingerprint = "fp_safe_user";

        // Simulate 10 requests (under the limit of 30)
        for (int i = 0; i < 10; i++) {
            boolean isSuspicious = rateLimitService.isSuspicious(testFingerprint);
            assertFalse(isSuspicious, "Request " + i + " should be allowed");
        }
    }

    @Test
    void shouldBlockRequestsOverLimit() {
        String testFingerprint = "fp_attacker";

        // Hit the limit (30 requests)
        for (int i = 0; i < 30; i++) {
            rateLimitService.isSuspicious(testFingerprint);
        }

        // The 31st request should be flagged as suspicious
        boolean isSuspicious = rateLimitService.isSuspicious(testFingerprint);
        assertTrue(isSuspicious, "Request 31 should be blocked");
    }
}