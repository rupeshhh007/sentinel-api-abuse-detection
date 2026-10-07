package com.rupesh.springpractice.detection;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;

public class RedisWindowStore implements WindowStore {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script = new DefaultRedisScript<>("""
        local t = redis.call('TIME')
        local now = tonumber(t[1])*1000 + math.floor(tonumber(t[2])/1000)
        local cutoff = now-tonumber(ARGV[1])*1000
        -- Source budget is checked first: header rotation cannot allocate unlimited client keys.
        redis.call('ZREMRANGEBYSCORE',KEYS[2],'-inf',cutoff)
        local sourceCount = redis.call('ZCARD',KEYS[2])
        if sourceCount <= tonumber(ARGV[5]) then
          redis.call('ZADD',KEYS[2],now,ARGV[2])
          sourceCount = redis.call('ZCARD',KEYS[2])
        end
        redis.call('EXPIRE',KEYS[2],tonumber(ARGV[1]))
        if sourceCount > tonumber(ARGV[5]) then
          redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',cutoff)
          return {tostring(redis.call('ZCARD',KEYS[1])),tostring(sourceCount)}
        end
        redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',cutoff)
        local clientCount = redis.call('ZCARD',KEYS[1])
        if clientCount <= tonumber(ARGV[4]) then
          redis.call('ZADD',KEYS[1],now,ARGV[2])
          clientCount = redis.call('ZCARD',KEYS[1])
        end
        redis.call('EXPIRE',KEYS[1],tonumber(ARGV[1]))
        redis.call('RPUSH',KEYS[3],now..'::'..ARGV[3])
        redis.call('LTRIM',KEYS[3],-20,-1)
        redis.call('EXPIRE',KEYS[3],tonumber(ARGV[1]))
        local result = {tostring(clientCount),tostring(sourceCount)}
        local events = redis.call('LRANGE',KEYS[3],0,-1)
        for _,event in ipairs(events) do table.insert(result,event) end
        return result
        """, List.class);
    public RedisWindowStore(StringRedisTemplate redis) { this.redis=redis; }
    @Override public boolean healthy() {
        try (var connection = Objects.requireNonNull(redis.getConnectionFactory()).getConnection()) {
            return "PONG".equals(connection.ping());
        } catch (org.springframework.dao.DataAccessException e) { return false; }
    }
    @Override public Window record(String client,String source,long now,String endpoint,Policy policy) {
        // All keys for a source share a Redis Cluster hash slot.
        String prefix = "sentinel:{"+source+"}:";
        List<?> result = redis.execute(script,List.of(prefix+client+":rate",prefix+"source",prefix+client+":history"),
            String.valueOf(policy.windowSeconds()),UUID.randomUUID().toString(),endpoint,
            String.valueOf(policy.maxRequests()),String.valueOf(policy.sourceMaxRequests()));
        if (result==null || result.size()<2) throw new IllegalStateException("Redis returned no decision window");
        List<Sample> events=new ArrayList<>();
        long latest = result.size()>2 ? Long.parseLong(result.get(result.size()-1).toString().split("::",2)[0]) : now;
        for(int i=2;i<result.size();i++) {
            String[] parts=result.get(i).toString().split("::",2);
            long time=Long.parseLong(parts[0]);
            if(time>latest-policy.windowSeconds()*1000L) events.add(new Sample(time,parts[1]));
        }
        return new Window(Long.parseLong(result.get(0).toString()),Long.parseLong(result.get(1).toString()),events);
    }
}
