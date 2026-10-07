package com.rupesh.springpractice.detection;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import static com.rupesh.springpractice.detection.DetectionModel.*;
@Service
public class PolicyService {
    private volatile Policy policy;
    public PolicyService(@Value("${sentinel.rate-limit.max-requests:30}") int max,
                         @Value("${sentinel.rate-limit.window-seconds:60}") int seconds,
                         @Value("${sentinel.risk.threshold:60}") int threshold) {
        policy=new Policy(1,"ENFORCE",max,Math.max(120,max*4),seconds,threshold,"OPEN");
    }
    public Policy get() {return policy;}
    public synchronized Policy update(Policy next) {
        if(next.version()!=policy.version()) throw new IllegalStateException("Policy changed; refresh before saving");
        policy=new Policy(policy.version()+1,next.mode(),next.maxRequests(),next.sourceMaxRequests(),next.windowSeconds(),next.threshold(),next.failureMode());
        return policy;
    }
}
