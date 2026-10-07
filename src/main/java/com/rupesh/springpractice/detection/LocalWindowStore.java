package com.rupesh.springpractice.detection;

import java.util.*;
import static com.rupesh.springpractice.detection.DetectionModel.*;

/** Bounded, isolated demo state. Never used as an implicit Redis fallback. */
public class LocalWindowStore implements WindowStore {
    private final Map<String,Deque<Sample>> clients = new LinkedHashMap<>();
    private final Map<String,Deque<Sample>> histories = new HashMap<>();
    private final Map<String,Deque<Long>> sources = new LinkedHashMap<>();
    @Override public synchronized Window record(String client, String source, long now, String endpoint, Policy policy) {
        if (!sources.containsKey(source) && sources.size() >= 2000) sources.remove(sources.keySet().iterator().next());
        Deque<Long> ip = sources.computeIfAbsent(source,k->new ArrayDeque<>());
        long cutoff = now-policy.windowSeconds()*1000L;
        ip.removeIf(t->t<=cutoff);
        if (ip.size() <= policy.sourceMaxRequests()) ip.addLast(now);
        if (ip.size() > policy.sourceMaxRequests()) {
            Deque<Sample> existing=clients.get(client);
            if(existing!=null)existing.removeIf(event->event.time()<=cutoff);
            return new Window(existing==null?0:existing.size(),ip.size(),List.of());
        }
        if (!clients.containsKey(client) && clients.size() >= 2000) {
            String oldest=clients.keySet().iterator().next();clients.remove(oldest);histories.remove(oldest);
        }
        Deque<Sample> events = clients.computeIfAbsent(client,k->new ArrayDeque<>());
        events.removeIf(s->s.time()<=cutoff);
        // Saturate above the budget; memory does not grow with an unbounded flood.
        if (events.size() <= policy.maxRequests()) events.addLast(new Sample(now,endpoint));
        Deque<Sample> recent=histories.computeIfAbsent(client,k->new ArrayDeque<>());
        recent.removeIf(s->s.time()<=cutoff);
        if(recent.size()>=20)recent.removeFirst();
        recent.addLast(new Sample(now,endpoint));
        List<Sample> history = List.copyOf(recent);
        return new Window(events.size(), ip.size(), history);
    }
}
