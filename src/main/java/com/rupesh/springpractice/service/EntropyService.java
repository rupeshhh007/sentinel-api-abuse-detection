package com.rupesh.springpractice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EntropyService {

    private static final int MAX_EVENTS = 20;
    private static final int TTL_SECONDS = 600; // 10 minutes

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> entropyScript;

    public EntropyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // This script executes 4 commands atomically on the Redis server
        // ARGV[1] = event data, ARGV[2] = max events, ARGV[3] = ttl
        String luaScript =
                "redis.call('RPUSH', KEYS[1], ARGV[1]) " +
                        "redis.call('LTRIM', KEYS[1], -tonumber(ARGV[2]), -1) " +
                        "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3])) " +
                        "return redis.call('LRANGE', KEYS[1], 0, -1)";

        // We expect a List of Strings back from Redis
        this.entropyScript = new DefaultRedisScript<>(luaScript, List.class);
    }

    public EntropyLevel recordAndEvaluate(String fingerprint, long timestamp, String endpoint) {
        String key = "entropy:" + fingerprint;
        // Combine timestamp and endpoint into a single payload
        String eventData = timestamp + "::" + endpoint;

        // Execute the script: 1 network round-trip instead of 8
        List<String> events = redisTemplate.execute(
                entropyScript,
                Collections.singletonList(key),
                eventData,
                String.valueOf(MAX_EVENTS),
                String.valueOf(TTL_SECONDS)
        );

        if (events == null || events.size() < 5) {
            return EntropyLevel.UNKNOWN; // Not enough data yet
        }

        List<Long> times = new ArrayList<>();
        List<String> endpoints = new ArrayList<>();

        // Parse the combined data back out for the calculation logic
        for (String event : events) {
            String[] parts = event.split("::", 2);
            if (parts.length == 2) {
                times.add(Long.valueOf(parts[0]));
                endpoints.add(parts[1]);
            }
        }

        return calculateEntropy(times, endpoints);
    }

    private EntropyLevel calculateEntropy(List<Long> times, List<String> endpoints) {
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
            return EntropyLevel.LOW; // Very robotic behavior
        }

        if (gaps.size() <= 4 && uniqueEndpoints.size() <= 2) {
            return EntropyLevel.MEDIUM; // Somewhat predictable
        }

        return EntropyLevel.HIGH; // Human-like randomness
    }
}