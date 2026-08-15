package com.rupesh.springpractice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntropyService {

    private static final int MAX_EVENTS = 20;

    private final StringRedisTemplate redisTemplate;

    public EntropyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public EntropyLevel recordAndEvaluate(String fingerprint, long timestamp, String endpoint) {
        String timesKey = "entropy:times:" + fingerprint;
        String endpointsKey = "entropy:endpoints:" + fingerprint;

        // 1. Push timestamp and maintain a capped list of MAX_EVENTS
        redisTemplate.opsForList().rightPush(timesKey, String.valueOf(timestamp));
        redisTemplate.opsForList().trim(timesKey, -MAX_EVENTS, -1);
        redisTemplate.expire(timesKey, Duration.ofMinutes(10));

        // 2. Push endpoint and maintain a capped list of MAX_EVENTS
        redisTemplate.opsForList().rightPush(endpointsKey, endpoint);
        redisTemplate.opsForList().trim(endpointsKey, -MAX_EVENTS, -1);
        redisTemplate.expire(endpointsKey, Duration.ofMinutes(10));

        // 3. Fetch current state of lists
        List<String> timesStr = redisTemplate.opsForList().range(timesKey, 0, -1);
        List<String> endpoints = redisTemplate.opsForList().range(endpointsKey, 0, -1);

        if (timesStr == null || endpoints == null) {
            return EntropyLevel.UNKNOWN;
        }

        List<Long> times = timesStr.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        return calculateEntropy(times, endpoints);
    }

    private EntropyLevel calculateEntropy(List<Long> times, List<String> endpoints) {
        if (times.size() < 5) {
            return EntropyLevel.UNKNOWN; // not enough data
        }

        // ---- Time gap diversity ----
        Set<Long> gaps = new HashSet<>();
        Long prev = null;
        for (Long t : times) {
            if (prev != null) {
                gaps.add(t - prev);
            }
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
