package com.rupesh.springpractice.detection;

import java.util.List;

public final class DetectionModel {
    private DetectionModel() {}
    public record Sample(long time, String endpoint) {}
    public record Window(long clientCount, long sourceCount, List<Sample> samples) {}
    public record Signal(String name, double value, int points, String evidence) {}
    public record Assessment(int score, boolean abusive, int samples, List<Signal> signals) {}
    public record Decision(String id, long timestamp, String client, String source, String endpoint, String method,
                           int score, String action, String origin, double latencyMs, int samples,
                           long policyVersion, Policy policy, List<Signal> signals) {}
    public record Policy(long version, String mode, int maxRequests, int sourceMaxRequests,
                         int windowSeconds, int threshold, String failureMode) {
        public Policy {
            if (!List.of("ENFORCE", "OBSERVE").contains(mode) || !List.of("OPEN", "CLOSED").contains(failureMode)
                || maxRequests < 5 || maxRequests > 10000 || sourceMaxRequests < maxRequests
                || sourceMaxRequests > 100000 || windowSeconds < 10 || windowSeconds > 300
                || threshold < 40 || threshold > 100) throw new IllegalArgumentException("Invalid policy bounds or mode");
        }
    }
}
