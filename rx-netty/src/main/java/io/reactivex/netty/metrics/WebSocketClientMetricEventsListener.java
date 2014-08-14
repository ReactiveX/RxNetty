package io.reactivex.netty.metrics;

import java.util.concurrent.TimeUnit;

import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientMetricsEvent;

/**
 * A convenience implementation for {@link io.reactivex.netty.metrics.MetricEventsListener} for receiving {@link WebSocketClientMetricsEvent}. This
 * implementation receives the events and provides convenience methods representing those events with clear arguments
 * that are expected with that event type.
 *
 * @author Tomasz Bak
 */
public class WebSocketClientMetricEventsListener extends ClientMetricEventsListener<ClientMetricsEvent<?>> {

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType() instanceof ClientMetricsEvent.EventType) {
            super.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            switch ((WebSocketClientMetricsEvent.EventType) event.getType()) {
                case HandshakeStart:
                    onHandshakeStart();
                    break;
                case HandshakeSuccess:
                    onHandshakeSuccess(duration, timeUnit);
                    break;
                case HandshakeFailure:
                    onHandshakeFailure(duration, timeUnit, throwable);
                    break;
                case WebSocketFrameWrites:
                    onWebSocketWrites();
                    break;
                case WebSocketFrameReads:
                    onWebSocketReads();
                    break;
            }
        }
    }

    @SuppressWarnings("unused")
    protected void onHandshakeStart() {
    }

    @SuppressWarnings("unused")
    protected void onHandshakeSuccess(long duration, TimeUnit timeUnit) {
    }

    @SuppressWarnings("unused")
    protected void onHandshakeFailure(long duration, TimeUnit timeUnit, Throwable throwable) {
    }

    @SuppressWarnings("unused")
    protected void onWebSocketWrites() {
    }

    @SuppressWarnings("unused")
    protected void onWebSocketReads() {
    }
}
