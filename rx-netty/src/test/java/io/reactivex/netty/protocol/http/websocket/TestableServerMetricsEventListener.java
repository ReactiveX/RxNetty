package io.reactivex.netty.protocol.http.websocket;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.netty.metrics.EventInvocationsStore;
import io.reactivex.netty.metrics.MetricsEvent;
import io.reactivex.netty.metrics.WebSocketServerMetricEventsListener;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Tomasz Bak
 */
public class TestableServerMetricsEventListener extends WebSocketServerMetricEventsListener {

    private final EventInvocationsStore<WebSocketServerMetricsEvent.EventType> webSocketEventStore;

    TestableServerMetricsEventListener() {
        webSocketEventStore = new EventInvocationsStore<WebSocketServerMetricsEvent.EventType>(WebSocketServerMetricsEvent.EventType.class);
    }

    public EnumMap<WebSocketServerMetricsEvent.EventType, Integer> getEventTypeVsInvocations() {
        return webSocketEventStore.getEventTypeVsInvocations();
    }

    public EnumMap<WebSocketServerMetricsEvent.EventType, List<String>> getEventTypeVsInvalidInvocations() {
        return webSocketEventStore.getEventTypeVsInvalidInvocations();
    }

    @Override
    public void onEvent(ServerMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        if (WebSocketServerMetricsEvent.EventType.class == event.getType().getClass()) {
            webSocketEventStore.onEvent((MetricsEvent<WebSocketServerMetricsEvent.EventType>) event, duration, timeUnit, throwable, value);
        }
    }
}
