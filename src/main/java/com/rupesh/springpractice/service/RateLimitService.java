package com.rupesh.springpractice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Service
public class RateLimitService {

    private final int windowSeconds;
    private final int maxRequests;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimitService(
            StringRedisTemplate redisTemplate,
            @Value("${sentinel.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${sentinel.rate-limit.max-requests:30}") int maxRequests
    ) {
        this.redisTemplate = redisTemplate;
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;

        String luaScript =
                "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', tonumber(ARGV[2])) " +
                        "redis.call('ZADD', KEYS[1], tonumber(ARGV[1]), ARGV[3]) " +
                        "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4])) " +
                        "local count = redis.call('ZCARD', KEYS[1]) " +
                        "if count > tonumber(ARGV[5]) then " +
                        "   return 1 " +
                        "else " +
                        "   return 0 " +
                        "end";

        this.rateLimitScript = new DefaultRedisScript<>(luaScript, Long.class);
    }

    public boolean isSuspicious(String fingerprint) {
        String key = "ratelimit:" + fingerprint;
        long nowMilli = Instant.now().toEpochMilli();
        long windowStartMilli = nowMilli - (windowSeconds * 1000L);
        String uniqueId = UUID.randomUUID().toString();

        Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(nowMilli),
                String.valueOf(windowStartMilli),
                uniqueId,
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests)
        );

        return result != null && result == 1L;
    }
}