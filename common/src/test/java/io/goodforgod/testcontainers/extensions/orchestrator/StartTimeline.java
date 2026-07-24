package io.goodforgod.testcontainers.extensions.orchestrator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StartTimeline {

    public record Event(long start, long end) {}

    private static final Map<String, Event> EVENTS = new ConcurrentHashMap<>();

    private StartTimeline() {}

    public static void clear() {
        EVENTS.clear();
    }

    public static void started(String service, long startedAt, long endedAt) {
        EVENTS.put(service, new Event(startedAt, endedAt));
    }

    public static void hooked(String service, String hook, long startedAt, long endedAt) {
        EVENTS.put(hook + ":" + service, new Event(startedAt, endedAt));
    }

    public static Event event(String service) {
        return EVENTS.get(service);
    }
}
