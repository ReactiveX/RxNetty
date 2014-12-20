/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.spectator.http.websocket;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.websocket.WebSocketClient;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientBuilder;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientMetricsEvent;
import io.reactivex.netty.protocol.http.websocket.WebSocketServer;
import io.reactivex.netty.protocol.http.websocket.WebSocketServerMetricsEvent;
import io.reactivex.netty.server.ServerMetricsEvent;
import io.reactivex.netty.spectator.SpectatorEventsListenerFactory;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Tomasz Bak
 */
public class WebSocketMetricsTest {

    private SpectatorEventsListenerFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new SpectatorEventsListenerFactory("websocket-metric-test-client", "websocket-metric-test-server");
    }

    @Test
    public void testWebSocketServerMetrics() throws Exception {
        WebSocketServer<WebSocketFrame, WebSocketFrame> server = RxNetty.newWebSocketServerBuilder(0, new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
            @Override
            public Observable<Void> handle(ObservableConnection<WebSocketFrame, WebSocketFrame> newConnection) {
                return Observable.empty();
            }
        }).build();
        MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject = server.getEventsSubject();

        WebSocketServerListener listener = factory.forWebSocketServer(server);
        server.subscribe(listener);

        eventsSubject.onEvent(ServerMetricsEvent.NEW_CLIENT_CONNECTED);
        assertEquals("Unexpected live connections.", 1, listener.getLiveConnections());

        eventsSubject.onEvent(WebSocketServerMetricsEvent.HANDSHAKE_PROCESSED);
        assertEquals("Expected one processed handshake", 1, listener.getProcessedHandshakes());

        eventsSubject.onEvent(WebSocketServerMetricsEvent.HANDSHAKE_FAILURE);
        assertEquals("Expected one failed handshake", 1, listener.getFailedHandshakes());
        assertEquals("Expected two processed connections", 2, listener.getProcessedHandshakes());

        eventsSubject.onEvent(WebSocketServerMetricsEvent.WEB_SOCKET_FRAME_WRITES);
        assertEquals("Expected one written frame", 1, listener.getWebSocketWrites());

        eventsSubject.onEvent(WebSocketServerMetricsEvent.WEB_SOCKET_FRAME_READS);
        assertEquals("Expected one read frame", 1, listener.getWebSocketReads());
    }

    @Test
    public void testWebSocketClientMetrics() throws Exception {
        WebSocketClientBuilder<WebSocketFrame, WebSocketFrame> builder = RxNetty.newWebSocketClientBuilder("localhost", 0);
        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = builder.getEventsSubject();
        WebSocketClient<WebSocketFrame, WebSocketFrame> client = builder.build();
        WebSocketClientListener listener = factory.forWebSocketClient(client);
        client.subscribe(listener);

        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
        assertEquals("Invalid pending connect count.", 1, listener.getPendingConnects());

        eventsSubject.onEvent(WebSocketClientMetricsEvent.HANDSHAKE_START);
        assertEquals("Expected one inflight handshake", 1, listener.getInflightHandshakes());
        eventsSubject.onEvent(WebSocketClientMetricsEvent.HANDSHAKE_SUCCESS, 10, TimeUnit.MILLISECONDS);
        assertEquals("Expected one processed handshake", 1, listener.getProcessedHandshakes());
        assertEquals("Expected none inflight handshake", 0, listener.getInflightHandshakes());
        assertEquals("Expected handshake processing time of 10ms", TimeUnit.MILLISECONDS.toNanos(10),
                listener.getHandshakeProcessingTimes().totalTime());

        eventsSubject.onEvent(WebSocketClientMetricsEvent.HANDSHAKE_START);
        assertEquals("Expected one inflight handshake", 1, listener.getInflightHandshakes());
        eventsSubject.onEvent(WebSocketClientMetricsEvent.HANDSHAKE_FAILURE, 10, TimeUnit.MILLISECONDS);
        assertEquals("Expected one failed handshake", 1, listener.getFailedHandshakes());
        assertEquals("Expected two processed connections", 2, listener.getProcessedHandshakes());
        assertEquals("Expected none inflight handshake", 0, listener.getInflightHandshakes());
        assertEquals("Expected handshake processing time of 10ms", TimeUnit.MILLISECONDS.toNanos(20),
                listener.getHandshakeProcessingTimes().totalTime());

        eventsSubject.onEvent(WebSocketClientMetricsEvent.WEB_SOCKET_FRAME_WRITES);
        assertEquals("Expected one written frame", 1, listener.getWebSocketWrites());

        eventsSubject.onEvent(WebSocketClientMetricsEvent.WEB_SOCKET_FRAME_READS);
        assertEquals("Expected one read frame", 1, listener.getWebSocketReads());
    }
}
