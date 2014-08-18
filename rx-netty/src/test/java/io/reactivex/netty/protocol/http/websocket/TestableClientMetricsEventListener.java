package io.reactivex.netty.protocol.http.websocket;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.EventInvocationsStore;
import io.reactivex.netty.metrics.MetricsEvent;
import io.reactivex.netty.metrics.WebSocketClientMetricEventsListener;

/**
 * @author Tomasz Bak
 */
public class TestableClientMetricsEventListener extends WebSocketClientMetricEventsListener {

    private final EventInvocationsStore<WebSocketClientMetricsEvent.EventType> webSocketEventStore;

    TestableClientMetricsEventListener() {
        webSocketEventStore = new EventInvocationsStore<WebSocketClientMetricsEvent.EventType>(WebSocketClientMetricsEvent.EventType.class);
    }

    public EnumMap<WebSocketClientMetricsEvent.EventType, Integer> getEventTypeVsInvocations() {
        return webSocketEventStore.getEventTypeVsInvocations();
    }

    public EnumMap<WebSocketClientMetricsEvent.EventType, List<String>> getEventTypeVsInvalidInvocations() {
        return webSocketEventStore.getEventTypeVsInvalidInvocations();
    }

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        if (WebSocketClientMetricsEvent.EventType.class == event.getType().getClass()) {
            webSocketEventStore.onEvent((MetricsEvent<WebSocketClientMetricsEvent.EventType>) event, duration, timeUnit, throwable, value);
        }
    }
}
