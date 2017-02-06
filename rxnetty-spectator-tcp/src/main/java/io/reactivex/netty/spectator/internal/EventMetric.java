package io.reactivex.netty.spectator.internal;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.histogram.PercentileTimer;

import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.spectator.internal.SpectatorUtils.*;

public class EventMetric {

    private final Counter start;
    private final PercentileTimer startLatency;

    private final Counter success;
    private final PercentileTimer successLatency;

    private final Counter failed;
    private final PercentileTimer failureLatency;

    public EventMetric(Registry registry, String name, String monitorId, String... tags) {
        start = newCounter(registry, name, monitorId, mergeTags(tags, "rtype", "count", "state", "start"));
        startLatency = newPercentileTimer(registry, name, monitorId, mergeTags(tags, "rtype", "latency",
                                                                               "state", "start"));
        success = newCounter(registry, name, monitorId, mergeTags(tags, "rtype", "count", "state", "success"));
        successLatency = newPercentileTimer(registry, name, monitorId, mergeTags(tags, "rtype", "latency",
                                                                                 "state", "success"));
        failed = newCounter(registry, name, monitorId, mergeTags(tags, "rtype", "count", "state", "failed"));
        failureLatency = newPercentileTimer(registry, name, monitorId, mergeTags(tags, "rtype", "latency",
                                                                               "state", "failed"));
    }

    public void start() {
        start.increment();
    }

    public void start(long duration, TimeUnit timeUnit) {
        start.increment();
        startLatency.record(duration, timeUnit);
    }

    public void success() {
        success.increment();
    }

    public void success(long duration, TimeUnit timeUnit) {
        success.increment();
        successLatency.record(duration, timeUnit);
    }

    public void failure() {
        failed.increment();
    }

    public void failure(long duration, TimeUnit timeUnit) {
        failed.increment();
        failureLatency.record(duration, timeUnit);
    }
}
