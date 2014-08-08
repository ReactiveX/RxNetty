package io.reactivex.netty.servo.http.websocket;

import java.util.concurrent.TimeUnit;

import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.Timer;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.WebSocketClientMetricEventsListener;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientMetricsEvent;
import io.reactivex.netty.servo.tcp.TcpClientListener;

import static com.netflix.servo.monitor.Monitors.*;
import static io.reactivex.netty.servo.ServoUtils.*;

/**
 * @author Tomasz Bak
 */
public class WebSocketClientListener extends TcpClientListener<ClientMetricsEvent<?>> {
    private final LongGauge inflightHandshakes;
    private final LongGauge processedHandshakes;
    private final LongGauge failedHandshakes;
    private final Counter webSocketWrites;
    private final Counter getWebSocketReads;
    private final Timer handshakeProcessingTimes;

    private final WebSocketClientMetricEventsListenerImpl delegate = new WebSocketClientMetricEventsListenerImpl();

    protected WebSocketClientListener(String monitorId) {
        super(monitorId);
        inflightHandshakes = newLongGauge("inflightHandshakes");
        processedHandshakes = newLongGauge("processedHandshakes");
        failedHandshakes = newLongGauge("failedHandshakes");
        webSocketWrites = newCounter("webSocketWrites");
        getWebSocketReads = newCounter("getWebSocketReads");
        handshakeProcessingTimes = newTimer("handshakeProcessingTimes");
    }

    public static WebSocketClientListener newWebSocketListener(String monitorId) {
        return new WebSocketClientListener(monitorId);
    }

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType().getClass() == WebSocketClientMetricsEvent.EventType.class) {
            delegate.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            super.onEvent(event, duration, timeUnit, throwable, value);
        }
    }

    public long getInflightHandshakes() {
        return inflightHandshakes.getNumber().get();
    }

    public long getProcessedHandshakes() {
        return processedHandshakes.getNumber().get();
    }

    public long getFailedHandshakes() {
        return failedHandshakes.getValue().longValue();
    }

    public Timer getHandshakeProcessingTimes() {
        return handshakeProcessingTimes;
    }

    public long getWebSocketWrites() {
        return webSocketWrites.getValue().longValue();
    }

    public long getWebSocketReads() {
        return getWebSocketReads.getValue().longValue();
    }

    private class WebSocketClientMetricEventsListenerImpl extends WebSocketClientMetricEventsListener {
        @Override
        protected void onHandshakeStart() {
            incrementLongGauge(inflightHandshakes);
        }

        @Override
        protected void onHandshakeSuccess(long duration, TimeUnit timeUnit) {
            decrementLongGauge(inflightHandshakes);
            incrementLongGauge(processedHandshakes);
            handshakeProcessingTimes.record(duration, timeUnit);
        }

        @Override
        protected void onHandshakeFailure(long duration, TimeUnit timeUnit, Throwable throwable) {
            decrementLongGauge(inflightHandshakes);
            incrementLongGauge(processedHandshakes);
            incrementLongGauge(failedHandshakes);
            handshakeProcessingTimes.record(duration, timeUnit);
        }

        @Override
        protected void onWebSocketWrites() {
            webSocketWrites.increment();
        }

        @Override
        protected void onWebSocketReads() {
            getWebSocketReads.increment();
        }
    }
}
