package com.rupesh.springpractice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RateLimitService {

    private static final int WINDOW_SECONDS = 60;
    private static final int MAX_REQUESTS = 30;

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isSuspicious(String fingerprint) {
        String key = "ratelimit:" + fingerprint;
        long nowMilli = Instant.now().toEpochMilli();
        long windowStartMilli = nowMilli - (WINDOW_SECONDS * 1000L);

        // Add current request with a unique value to prevent collisions in the ZSET
        redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), (double) nowMilli);

        // Remove old requests outside the sliding window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, (double) windowStartMilli);

        // Count remaining requests in the window
        Long count = redisTemplate.opsForZSet().zCard(key);

        // Set key TTL to keep Redis memory clean
        redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));

        return count != null && count > MAX_REQUESTS;
    }
}
