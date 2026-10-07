package com.rupesh.springpractice.detection;

import com.rupesh.springpractice.console.TelemetryService;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;

@Service
public class DecisionService {
    private final WindowStore store;
    private final BehaviorAnalyzer analyzer;
    private final PolicyService policies;
    private final TelemetryService telemetry;
    private final MeterRegistry metrics;
    public DecisionService(WindowStore store,BehaviorAnalyzer analyzer,PolicyService policies,TelemetryService telemetry,MeterRegistry metrics) {
        this.store=store;this.analyzer=analyzer;this.policies=policies;this.telemetry=telemetry;this.metrics=metrics;
    }
    public Decision evaluate(String client,String source,String endpoint,String method) {
        long start=System.nanoTime(), now=System.currentTimeMillis();
        Policy p=policies.get();
        Decision decision;
        try {
            Assessment a=analyzer.analyze(store.record(client,source,now,endpoint,p),p);
            decision=create(client,source,endpoint,method,now,a,p,"LIVE",start);
        } catch (org.springframework.dao.DataAccessException | IllegalStateException e) {
            metrics.counter("sentinel.redis.failures").increment();
            decision=new Decision(UUID.randomUUID().toString(),now,client.substring(0,Math.min(12,client.length())),source.substring(0,Math.min(12,source.length())),endpoint,method,0,
                p.failureMode().equals("CLOSED")?"UNAVAILABLE":"DEGRADED","LIVE",(System.nanoTime()-start)/1e6,0,p.version(),p,
                List.of(new Signal("Store unavailable",0,0,"Detection unavailable; failure mode "+p.failureMode())));
        }
        telemetry.add(decision);
        metrics.counter("sentinel.requests.evaluated").increment();
        if(decision.action().equals("BLOCK")) metrics.counter("sentinel.requests.blocked").increment();
        metrics.timer("sentinel.decision.latency").record(System.nanoTime()-start,java.util.concurrent.TimeUnit.NANOSECONDS);
        return decision;
    }
    public static Decision create(String client,String source,String endpoint,String method,long now,Assessment a,Policy p,String origin,long start) {
        String action=a.abusive()?(p.mode().equals("ENFORCE")?"BLOCK":"OBSERVE"):"ALLOW";
        return new Decision(UUID.randomUUID().toString(),now,client.substring(0,Math.min(12,client.length())),source.substring(0,Math.min(12,source.length())),endpoint,method,
            a.score(),action,origin,(System.nanoTime()-start)/1e6,a.samples(),p.version(),p,a.signals());
    }
}
