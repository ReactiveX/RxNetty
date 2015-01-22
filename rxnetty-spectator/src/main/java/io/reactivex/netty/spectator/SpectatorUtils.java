package io.reactivex.netty.spectator;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.ExtendedRegistry;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.api.Timer;

public final class SpectatorUtils {
    private SpectatorUtils() {
    }

    public static Counter newCounter(String name, String id) {
        return Spectator.registry().counter(name, "id", id);
    }

    public static Timer newTimer(String name, String id) {
        return Spectator.registry().timer(name, "id", id);
    }

    public static <T extends Number> T newGauge(String name, String id, T number) {
        final ExtendedRegistry registry = Spectator.registry();
        Id gaugeId = registry.createId(name, "id", id);
        return registry.gauge(gaugeId, number);
    }
}
