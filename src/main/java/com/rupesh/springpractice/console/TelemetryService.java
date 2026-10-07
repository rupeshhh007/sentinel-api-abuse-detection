package com.rupesh.springpractice.console;

import org.springframework.stereotype.Service;
import java.util.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;

@Service
public class TelemetryService {
    private final Deque<Decision> live=new ArrayDeque<>(), replay=new ArrayDeque<>();
    private final Map<String,long[]> counters=new HashMap<>();
    public synchronized void add(Decision d) {
        Deque<Decision> target=d.origin().equals("LIVE")?live:replay;
        if(target.size()>=1000) target.removeLast();
        target.addFirst(d);
        long[] count=counters.computeIfAbsent(d.origin(),k->new long[4]);
        count[0]++; if(d.action().equals("BLOCK")) count[1]++; if(d.action().equals("OBSERVE")) count[2]++;
        if(d.action().equals("DEGRADED")) count[3]++;
    }
    public synchronized Map<String,Object> snapshot(String origin) {
        List<Decision> events=List.copyOf(origin.equals("LIVE")?live:replay);
        long[] count=counters.getOrDefault(origin,new long[4]);
        double[] latencies=events.stream().mapToDouble(Decision::latencyMs).sorted().toArray();
        double p95=latencies.length==0?0:latencies[Math.min(latencies.length-1,(int)Math.ceil(latencies.length*.95)-1)];
        return Map.of("total",count[0],"blocked",count[1],"observed",count[2],"degraded",count[3],"p95Ms",p95,"events",events,"retention",1000,"origin",origin);
    }
    public synchronized Optional<Decision> find(String id) {
        return java.util.stream.Stream.concat(live.stream(),replay.stream()).filter(d->d.id().equals(id)).findFirst();
    }
    public synchronized void clearReplay() {replay.clear();counters.remove("REPLAY");}
}
