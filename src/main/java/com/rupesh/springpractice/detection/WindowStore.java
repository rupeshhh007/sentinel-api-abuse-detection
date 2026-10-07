package com.rupesh.springpractice.detection;
import static com.rupesh.springpractice.detection.DetectionModel.*;
public interface WindowStore {
    default boolean healthy() { return true; }
    Window record(String client, String source, long now, String endpoint, Policy policy);
}
