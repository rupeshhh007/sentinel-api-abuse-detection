package com.rupesh.springpractice.console;

import com.rupesh.springpractice.detection.*;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;

@Service
public class ReplayService {
    public record Scenario(String id,String name,String description,int requests,boolean malicious) {}
    public record ReplayResult(String scenario,int requests,int blocked,int observed,int suspicious,int firstDetection,
                               double decisionMs,Policy policy,List<Decision> decisions) {}
    private final BehaviorAnalyzer analyzer;
    private final PolicyService policies;
    private final TelemetryService telemetry;
    public ReplayService(BehaviorAnalyzer analyzer,PolicyService policies,TelemetryService telemetry) {
        this.analyzer=analyzer;this.policies=policies;this.telemetry=telemetry;
    }
    public List<Scenario> scenarios() {return List.of(
        new Scenario("human","Organic browsing","Variable timing, diverse routes. A benign control to reveal false positives.",36,false),
        new Scenario("scraper","Catalog scraper","Fixed 180ms cadence on one route. Repetition combines with rate pressure.",80,true),
        new Scenario("flood","Randomized flood","Irregular timing and rotating routes. Hard rate budgets catch evasive bursts.",100,true),
        new Scenario("rotation","Fingerprint rotation","One source rotates 16 client identities. Source aggregation closes the loophole.",180,true),
        new Scenario("monitor","Scheduled monitor","Regular polling below its budget. Periodicity alone must not cause a block.",18,false));}
    public ReplayResult run(String id,boolean publish) {
        Scenario scenario=scenarios().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown scenario"));
        return run(scenario,policies.get(),publish);
    }
    private ReplayResult run(Scenario s,Policy p,boolean publish) {
        LocalWindowStore store=new LocalWindowStore();
        List<Decision> events=new ArrayList<>();
        Random random=new Random(42);
        long now=System.currentTimeMillis()-240000;
        String run=UUID.randomUUID().toString().replace("-","");
        int first=0,blocked=0,observed=0,suspicious=0;
        long started=System.nanoTime();
        for(int i=0;i<s.requests();i++) {
            long gap=switch(s.id()) {case "human"->1500+random.nextInt(6000);case "monitor"->5000;case "scraper"->180;default->30+random.nextInt(250);};
            now+=gap;
            String endpoint=switch(s.id()) {case "scraper"->"/api/protected/catalog";case "monitor"->"/api/protected/status";default->List.of("/api/protected/catalog","/api/protected/orders","/api/protected/search","/api/protected/profile").get(random.nextInt(4));};
            String client=s.id().equals("rotation")?run.substring(0,8)+String.format("%04d",i%16):run;
            long start=System.nanoTime();
            Assessment a=analyzer.analyze(store.record(client,"isolated-source",now,endpoint,p),p);
            var d=DecisionService.create(client,run,endpoint,"GET",now,a,p,"REPLAY",start);
            events.add(d);if(publish)telemetry.add(d);
            if(a.abusive()) {suspicious++;if(first==0)first=i+1;}
            if(d.action().equals("BLOCK"))blocked++;
            if(d.action().equals("OBSERVE"))observed++;
        }
        return new ReplayResult(s.id(),s.requests(),blocked,observed,suspicious,first,(System.nanoTime()-started)/1e6,p,List.copyOf(events));
    }
    public Map<String,Object> evaluate() {
        Policy p=policies.get();
        List<Map<String,Object>> rows=new ArrayList<>();int tp=0,tn=0,fp=0,fn=0;
        for(Scenario s:scenarios()) {
            ReplayResult r=run(s,p,false);boolean detected=r.suspicious()>0;
            if(s.malicious()&&detected)tp++;if(!s.malicious()&&!detected)tn++;
            if(!s.malicious()&&detected)fp++;if(s.malicious()&&!detected)fn++;
            rows.add(Map.of("scenario",s.name(),"malicious",s.malicious(),"detected",detected,"firstDetection",r.firstDetection()));
        }
        return Map.of("truePositive",tp,"trueNegative",tn,"falsePositive",fp,"falseNegative",fn,"scenarios",rows,
            "policyVersion",p.version(),"note","Scenario-level synthetic evaluation, seed 42. Not a production accuracy estimate.");
    }
}
