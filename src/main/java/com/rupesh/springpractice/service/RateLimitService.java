package com.rupesh.springpractice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Service
public class RateLimitService {

    private static final int WINDOW_SECONDS = 60;
    private static final int MAX_REQUESTS = 30;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // We define the Lua script as a string.
        // Redis will execute this entire block as one atomic operation.
        String luaScript =
                "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', tonumber(ARGV[2])) " +
                        "redis.call('ZADD', KEYS[1], tonumber(ARGV[1]), ARGV[3]) " +
                        "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4])) " +
                        "local count = redis.call('ZCARD', KEYS[1]) " +
                        "if count > tonumber(ARGV[5]) then " +
                        "   return 1 " + // Suspicious
                        "else " +
                        "   return 0 " + // Safe
                        "end";

        // Initialize the script object, expecting a Long (1 or 0) back from Redis
        this.rateLimitScript = new DefaultRedisScript<>(luaScript, Long.class);
    }

    public boolean isSuspicious(String fingerprint) {
        String key = "ratelimit:" + fingerprint;
        long nowMilli = Instant.now().toEpochMilli();
        long windowStartMilli = nowMilli - (WINDOW_SECONDS * 1000L);
        String uniqueId = UUID.randomUUID().toString();

        // Execute the Lua script in Redis
        // Collections.singletonList(key) passes our key to KEYS[1] in the script
        // The remaining arguments map to ARGV[1] through ARGV[5] in the script
        Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(nowMilli),          // ARGV[1]
                String.valueOf(windowStartMilli),  // ARGV[2]
                uniqueId,                          // ARGV[3]
                String.valueOf(WINDOW_SECONDS),    // ARGV[4]
                String.valueOf(MAX_REQUESTS)       // ARGV[5]
        );

        // If the script returns 1, the rate limit was exceeded
        return result != null && result == 1L;
    }
}