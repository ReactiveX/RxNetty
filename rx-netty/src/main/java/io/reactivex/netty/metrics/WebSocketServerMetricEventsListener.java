package io.reactivex.netty.metrics;

import java.util.concurrent.TimeUnit;

import io.reactivex.netty.protocol.http.websocket.WebSocketServerMetricsEvent;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Tomasz Bak
 */
public class WebSocketServerMetricEventsListener extends ServerMetricEventsListener<ServerMetricsEvent<?>> {

    @Override
    public void onEvent(ServerMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType() instanceof ServerMetricsEvent.EventType) {
            super.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            switch ((WebSocketServerMetricsEvent.EventType) event.getType()) {
                case HandshakeProcessed:
                    onHandshakeProcessed();
                    break;
                case HandshakeFailure:
                    onHandshakeFailure();
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
    protected void onHandshakeProcessed() {
    }

    @SuppressWarnings("unused")
    protected void onHandshakeFailure() {
    }

    @SuppressWarnings("unused")
    protected void onWebSocketWrites() {
    }

    @SuppressWarnings("unused")
    protected void onWebSocketReads() {
    }
}
