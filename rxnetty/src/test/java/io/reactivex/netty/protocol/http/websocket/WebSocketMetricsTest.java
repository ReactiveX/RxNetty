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
package io.reactivex.netty.protocol.http.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author Tomasz Bak
 */
public class WebSocketMetricsTest {

    private RxServer<?, ?> server;
    private TestableServerMetricsEventListener serverEventListener;

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    public void setUpWebSocketServer() throws Exception {
        serverEventListener = new TestableServerMetricsEventListener();

        server = RxNetty.newWebSocketServerBuilder(0, new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
            @Override
            public Observable<Void> handle(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
                return connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(WebSocketFrame webSocketFrame) {
                        return connection.writeAndFlush(new TextWebSocketFrame("WS-Reply"));
                    }
                });
            }
        }).build();
        server.subscribe(serverEventListener);
        server.start();
    }

    public void setupPlainHttpServer() throws Exception {
        server = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                return Observable.empty();
            }
        }).start();
    }

    public TestableClientMetricsEventListener sendRequestReply() throws Exception {
        WebSocketClient<WebSocketFrame, WebSocketFrame> client = RxNetty.newWebSocketClientBuilder("localhost", server.getServerPort())
                .enableWireLogging(LogLevel.ERROR)
                .build();

        TestableClientMetricsEventListener eventListener = new TestableClientMetricsEventListener();
        client.subscribe(eventListener);

        client.connect().flatMap(new Func1<ObservableConnection<WebSocketFrame, WebSocketFrame>, Observable<Void>>() {
            @Override
            public Observable<Void> call(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
                return Observable.concat(
                        connection.writeAndFlush(new TextWebSocketFrame("WS-Request")),
                        connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(WebSocketFrame webSocketFrame) {
                                return connection.close();
                            }
                        }));
            }
        }).materialize().toBlocking().single();

        return eventListener;
    }

    @Test
    public void testWebSocketMetrics() throws Exception {
        setUpWebSocketServer();
        TestableClientMetricsEventListener clientEventListener = sendRequestReply();

        testClientEventFired(clientEventListener, WebSocketClientMetricsEvent.EventType.HandshakeStart, 1);
        testClientEventFired(clientEventListener, WebSocketClientMetricsEvent.EventType.HandshakeSuccess, 1);
        testClientEventNotFired(clientEventListener, WebSocketClientMetricsEvent.EventType.HandshakeFailure);
        testClientEventFired(clientEventListener, WebSocketClientMetricsEvent.EventType.WebSocketFrameReads, 1);
        testClientEventFired(clientEventListener, WebSocketClientMetricsEvent.EventType.WebSocketFrameWrites, 1);

        testServerEventFired(serverEventListener, WebSocketServerMetricsEvent.EventType.HandshakeProcessed, 1);
        testServerEventFired(serverEventListener, WebSocketServerMetricsEvent.EventType.WebSocketFrameReads, 1);
        testServerEventFired(serverEventListener, WebSocketServerMetricsEvent.EventType.WebSocketFrameWrites, 1);
    }

    @Test
    public void testHandshakeFailure() throws Exception {
        setupPlainHttpServer();
        TestableClientMetricsEventListener clientEventListener = sendRequestReply();

        testClientEventFired(clientEventListener, WebSocketClientMetricsEvent.EventType.HandshakeStart, 1);
        testClientEventNotFired(clientEventListener, WebSocketClientMetricsEvent.EventType.HandshakeSuccess);
        testClientEventFired(clientEventListener, WebSocketClientMetricsEvent.EventType.HandshakeFailure, 1);
        testClientEventNotFired(clientEventListener, WebSocketClientMetricsEvent.EventType.WebSocketFrameReads);
        testClientEventNotFired(clientEventListener, WebSocketClientMetricsEvent.EventType.WebSocketFrameWrites);
   }

    private static void testClientEventFired(TestableClientMetricsEventListener listener,
                                             WebSocketClientMetricsEvent.EventType eventType, int count) {
        Assert.assertTrue("WebSocket client event type: " + eventType + " not fired",
                listener.getEventTypeVsInvocations().containsKey(eventType));
        if (-1 != count) {
            Assert.assertEquals("WebSocket client event type: " + eventType + " not fired as many times as expected.",
                    count, (long) listener.getEventTypeVsInvocations().get(eventType));
        }
    }

    private static void testClientEventNotFired(TestableClientMetricsEventListener listener, WebSocketClientMetricsEvent.EventType eventType) {
        Assert.assertTrue("WebSocket client event type: " + eventType + " fired, when not expected",
                !listener.getEventTypeVsInvocations().containsKey(eventType));
    }

    private static void testServerEventFired(TestableServerMetricsEventListener listener,
                                             WebSocketServerMetricsEvent.EventType eventType, int count) {
        Assert.assertTrue("WebSocket server event type: " + eventType + " not fired",
                listener.getEventTypeVsInvocations().containsKey(eventType));
        if (-1 != count) {
            Assert.assertEquals("WebSocket server event type: " + eventType + " not fired as many times as expected.",
                    count, (long) listener.getEventTypeVsInvocations().get(eventType));
        }
    }
}
