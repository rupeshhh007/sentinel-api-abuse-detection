package com.rupesh.springpractice.service;

import com.rupesh.springpractice.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntropyServiceIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    private EntropyService entropyService;

    @Test
    void shouldReturnUnknownWhenNotEnoughData() {
        String fingerprint = "fp_new_user";
        long now = System.currentTimeMillis() / 1000;

        // Only 2 requests (need at least 5)
        entropyService.recordAndEvaluate(fingerprint, now, "/api/data1");
        EntropyLevel level = entropyService.recordAndEvaluate(fingerprint, now + 1, "/api/data2");

        assertEquals(EntropyLevel.UNKNOWN, level);
    }

    @Test
    void shouldDetectLowEntropyBotBehavior() {
        String fingerprint = "fp_bot";
        long startTime = System.currentTimeMillis() / 1000;

        EntropyLevel finalLevel = EntropyLevel.UNKNOWN;

        // Simulate a bot hitting the exact same endpoint with perfect timing (1s gaps)
        for (int i = 0; i < 6; i++) {
            finalLevel = entropyService.recordAndEvaluate(fingerprint, startTime + i, "/api/login");
        }

        assertEquals(EntropyLevel.LOW, finalLevel);
    }

    @Test
    void shouldDetectHighEntropyHumanBehavior() {
        String fingerprint = "fp_human";
        long startTime = System.currentTimeMillis() / 1000;

        EntropyLevel finalLevel = EntropyLevel.UNKNOWN;

        // Simulate a human hitting different pages at varying time intervals
        finalLevel = entropyService.recordAndEvaluate(fingerprint, startTime, "/");
        finalLevel = entropyService.recordAndEvaluate(fingerprint, startTime + 10, "/about");
        finalLevel = entropyService.recordAndEvaluate(fingerprint, startTime + 25, "/products");
        finalLevel = entropyService.recordAndEvaluate(fingerprint, startTime + 28, "/products/1");
        finalLevel = entropyService.recordAndEvaluate(fingerprint, startTime + 60, "/cart");

        assertEquals(EntropyLevel.HIGH, finalLevel);
    }
}