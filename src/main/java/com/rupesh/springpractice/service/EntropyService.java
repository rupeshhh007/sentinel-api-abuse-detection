package com.rupesh.springpractice.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EntropyService {

    private static final int MAX_EVENTS = 20;

    private final Map<String, Deque<Long>> timeMap = new ConcurrentHashMap<>();
    private final Map<String, Deque<String>> endpointMap = new ConcurrentHashMap<>();

    public EntropyLevel recordAndEvaluate(String fingerprint, long timestamp, String endpoint) {

        Deque<Long> times =
                timeMap.computeIfAbsent(fingerprint, k -> new ArrayDeque<>());
        Deque<String> endpoints =
                endpointMap.computeIfAbsent(fingerprint, k -> new ArrayDeque<>());

        synchronized (times) {
            times.addLast(timestamp);
            if (times.size() > MAX_EVENTS) times.removeFirst();
        }

        synchronized (endpoints) {
            endpoints.addLast(endpoint);
            if (endpoints.size() > MAX_EVENTS) endpoints.removeFirst();
        }

        return calculateEntropy(times, endpoints);
    }

    private EntropyLevel calculateEntropy(Deque<Long> times, Deque<String> endpoints) {

        if (times.size() < 5) {
            return EntropyLevel.UNKNOWN; // not enough data
        }

        // ---- Time gap diversity ----
        Set<Long> gaps = new HashSet<>();
        Long prev = null;
        for (Long t : times) {
            if (prev != null) gaps.add(t - prev);
            prev = t;
        }

        // ---- Endpoint diversity ----
        Set<String> uniqueEndpoints = new HashSet<>(endpoints);

        // Simple heuristic (explainable)
        if (gaps.size() <= 2 && uniqueEndpoints.size() <= 1) {
            return EntropyLevel.LOW;
        }

        if (gaps.size() <= 4 && uniqueEndpoints.size() <= 2) {
            return EntropyLevel.MEDIUM;
        }

        return EntropyLevel.HIGH;
    }
}
