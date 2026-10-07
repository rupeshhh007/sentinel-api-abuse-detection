package com.rupesh.springpractice.detection;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
@Configuration
public class DetectionConfiguration {
    @Bean @Profile("!demo") WindowStore redisWindowStore(StringRedisTemplate redis) { return new RedisWindowStore(redis); }
    @Bean @Profile("demo") WindowStore demoWindowStore() { return new LocalWindowStore(); }
}
