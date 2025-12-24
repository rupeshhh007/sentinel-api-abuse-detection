package com.rupesh.springpractice.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int WINDOW_SECONDS = 60;
    private static final int MAX_REQUESTS = 30;

    private final Map<String, Deque<Long>> requestMap = new ConcurrentHashMap<>();

    public boolean isSuspicious(String fingerprint) {
        long now = Instant.now().getEpochSecond();

        Deque<Long> timestamps =
                requestMap.computeIfAbsent(fingerprint, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            timestamps.addLast(now);

            // Remove timestamps outside the window
            while (!timestamps.isEmpty()
                    && timestamps.peekFirst() <= now - WINDOW_SECONDS) {
                timestamps.removeFirst();
            }

            return timestamps.size() > MAX_REQUESTS;
        }
    }
}
