package com.rupesh.springpractice.detection;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;
class BehaviorAnalyzerTest {
    private final BehaviorAnalyzer analyzer=new BehaviorAnalyzer();
    private final Policy policy=new Policy(1,"ENFORCE",30,120,60,60,"OPEN");
    @Test void periodicPollingBelowBudgetDoesNotBlock() {
        List<Sample> samples=new ArrayList<>();for(int i=0;i<20;i++)samples.add(new Sample(i*1000L,"/status"));
        Assessment a=analyzer.analyze(new Window(20,20,samples),policy);
        assertEquals(50,a.score());assertFalse(a.abusive());
    }
    @Test void randomFloodCannotBypassHardBudget() {
        Assessment a=analyzer.analyze(new Window(31,31,List.of()),policy);
        assertEquals(60,a.score());assertTrue(a.abusive());
    }
    @Test void sourceAggregationDetectsFingerprintRotation() {
        Assessment a=analyzer.analyze(new Window(8,121,List.of()),policy);assertTrue(a.abusive());
    }
    @Test void millisecondVariationIsPreserved() {
        var a=analyzer.analyze(new Window(5,5,List.of(new Sample(0,"/a"),new Sample(20,"/b"),new Sample(110,"/c"),new Sample(130,"/d"),new Sample(330,"/a"))),policy);
        assertEquals(0,a.score());
    }
    @Test void localWindowExpiresAtBoundaryAndDoesNotGrowWithoutBound() {
        LocalWindowStore store=new LocalWindowStore();Window w=null;
        for(int i=0;i<1000;i++)w=store.record("client","source",1000,"/a",policy);
        assertEquals(31,w.clientCount());assertEquals(121,w.sourceCount());assertEquals(0,w.samples().size());
        w=store.record("client","source",61000,"/b",policy);
        assertEquals(1,w.clientCount());assertEquals(1,w.sourceCount());
    }
    @Test void policyRejectsInvalidAndConflictingWrites() {
        assertThrows(IllegalArgumentException.class,()->new Policy(1,"ENFORCE",30,5,60,60,"OPEN"));
        PolicyService service=new PolicyService(30,60,60);service.update(policy);
        assertThrows(IllegalStateException.class,()->service.update(policy));
    }
}
