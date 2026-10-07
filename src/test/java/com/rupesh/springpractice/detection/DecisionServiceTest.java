package com.rupesh.springpractice.detection;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.rupesh.springpractice.console.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;
import static org.junit.jupiter.api.Assertions.*;
class DecisionServiceTest {
    @Test void failureModeIsExplicitAndObserved() {
        WindowStore failed=(c,s,t,e,p)->{throw new DataAccessResourceFailureException("offline");};
        PolicyService policy=new PolicyService(30,60,60);TelemetryService telemetry=new TelemetryService();
        DecisionService service=new DecisionService(failed,new BehaviorAnalyzer(),policy,telemetry,new SimpleMeterRegistry());
        assertEquals("DEGRADED",service.evaluate("0123456789abcdef","source","/a","GET").action());
        policy.update(new Policy(1,"ENFORCE",30,120,60,60,"CLOSED"));
        assertEquals("UNAVAILABLE",service.evaluate("0123456789abcdef","source","/a","GET").action());
        assertEquals(2L,telemetry.snapshot("LIVE").get("total"));
    }
    @Test void syntheticEvaluationIncludesBenignControlsAndDoesNotPolluteTelemetry() {
        TelemetryService telemetry=new TelemetryService();PolicyService policies=new PolicyService(30,60,60);
        ReplayService replay=new ReplayService(new BehaviorAnalyzer(),policies,telemetry);
        var result=replay.evaluate();assertEquals(3,result.get("truePositive"));assertEquals(2,result.get("trueNegative"));
        assertEquals(0,result.get("falsePositive"));assertEquals(0,result.get("falseNegative"));
        assertEquals(0L,telemetry.snapshot("REPLAY").get("total"));
        policies.update(new Policy(1,"OBSERVE",30,120,60,60,"OPEN"));
        var r=replay.run("scraper",true);assertEquals(0,r.blocked());assertTrue(r.observed()>0);
    }
}
