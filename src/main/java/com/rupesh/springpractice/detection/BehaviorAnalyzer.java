package com.rupesh.springpractice.detection;

import org.springframework.stereotype.Component;
import java.util.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;

@Component
public class BehaviorAnalyzer {
    public Assessment analyze(Window window, Policy policy) {
        List<Sample> samples = window.samples();
        boolean rate = window.clientCount() > policy.maxRequests();
        boolean sourceRate = window.sourceCount() > policy.sourceMaxRequests();
        List<Signal> signals = new ArrayList<>();
        signals.add(new Signal("Client rate", (double) window.clientCount() / policy.maxRequests(), rate ? 40 : 0,
            (rate ? "≥" : "") + window.clientCount() + " requests / " + policy.windowSeconds() + "s; budget " + policy.maxRequests()));
        signals.add(new Signal("Source rate", (double) window.sourceCount() / policy.sourceMaxRequests(), sourceRate ? 60 : 0,
            (sourceRate ? "≥" : "") + window.sourceCount() + " requests across fingerprints; budget " + policy.sourceMaxRequests()));
        double cv = 1;
        double concentration = 0;
        if (samples.size() >= 5) {
            double[] gaps = new double[samples.size()-1];
            for (int i=1;i<samples.size();i++) gaps[i-1] = Math.max(0, samples.get(i).time()-samples.get(i-1).time());
            double mean = Arrays.stream(gaps).average().orElse(0);
            double variance = Arrays.stream(gaps).map(g -> (g-mean)*(g-mean)).average().orElse(0);
            cv = mean == 0 ? 0 : Math.sqrt(variance)/mean;
            Map<String, Integer> counts = new HashMap<>();
            samples.forEach(s -> counts.merge(s.endpoint(),1,Integer::sum));
            concentration = (double) Collections.max(counts.values()) / samples.size();
        }
        boolean regular = samples.size() >= 5 && cv < 0.15;
        boolean repetitive = samples.size() >= 5 && concentration >= 0.85;
        signals.add(new Signal("Timing regularity", Math.max(0, 1-Math.min(cv,1)), regular ? 30 : 0,
            samples.size() < 5 ? "Warming up: need 5 samples" : String.format(Locale.ROOT, "Gap coefficient of variation %.3f; trigger < 0.15",cv)));
        signals.add(new Signal("Endpoint repetition", concentration, repetitive ? 20 : 0,
            samples.size() < 5 ? "Insufficient history" : String.format(Locale.ROOT,"Dominant route %.0f%%; trigger ≥ 85%%", concentration*100)));
        int score = Math.min(100,signals.stream().mapToInt(Signal::points).sum());
        // Rate budgets are hard constraints: a randomized flood must not bypass enforcement.
        if (rate || sourceRate) score = Math.max(score,policy.threshold());
        return new Assessment(score,rate || sourceRate || score >= policy.threshold(), samples.size(),List.copyOf(signals));
    }
}
