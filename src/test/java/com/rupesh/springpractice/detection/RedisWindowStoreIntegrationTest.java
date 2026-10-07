package com.rupesh.springpractice.detection;
import com.rupesh.springpractice.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;
class RedisWindowStoreIntegrationTest extends AbstractRedisIntegrationTest {
 @Autowired StringRedisTemplate redis;
 @Test void atomicConcurrentWindowPreservesBudgetAndCapsState() throws Exception {
  RedisWindowStore store=new RedisWindowStore(redis);Policy p=new Policy(1,"ENFORCE",30,120,60,60,"OPEN");
  String source=UUID.randomUUID().toString();ExecutorService pool=Executors.newFixedThreadPool(12);
  try {
   List<Callable<Window>> calls=new ArrayList<>();for(int i=0;i<100;i++)calls.add(()->store.record("client",source,System.currentTimeMillis(),"/catalog",p));
   List<Future<Window>> futures=pool.invokeAll(calls);long allowed=0;
   for(Future<Window> f:futures)if(f.get().clientCount()<=30)allowed++;
   assertEquals(30,allowed);
   var w=store.record("client",source,System.currentTimeMillis(),"/catalog",p);
   assertEquals(31,w.clientCount());assertEquals(101,w.sourceCount());assertEquals(20,w.samples().size());
   assertEquals(31L,redis.opsForZSet().zCard("sentinel:{"+source+"}:client:rate"));
   assertTrue(redis.getExpire("sentinel:{"+source+"}:client:rate")>0);
  }finally{pool.shutdownNow();}
 }
 @Test void exhaustedSourceDoesNotAllocateNewFingerprintKeys() {
  RedisWindowStore store=new RedisWindowStore(redis);Policy p=new Policy(1,"ENFORCE",5,5,60,60,"OPEN");
  String source=UUID.randomUUID().toString();
  for(int i=0;i<10;i++) store.record("rotating-"+i,source,0,"/catalog",p);
  assertFalse(Boolean.TRUE.equals(redis.hasKey("sentinel:{"+source+"}:rotating-9:rate")));
  assertEquals(6L,redis.opsForZSet().zCard("sentinel:{"+source+"}:source"));
 }
}
