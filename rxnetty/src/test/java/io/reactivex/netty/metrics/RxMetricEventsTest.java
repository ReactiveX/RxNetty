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

package io.reactivex.netty.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.ChannelCloseListener;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientMetricsEvent;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerMetricsEvent;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.ServerMetricsEvent;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class RxMetricEventsTest {

    @Test
    public void testMetricEventsSanityCheck() throws Exception {
        final CountDownLatch bothClientAndServerDone = new CountDownLatch(2);
        final ChannelCloseListener serverCloseListener = new ChannelCloseListener();
        HttpServer<ByteBuf, ByteBuf> server =
                RxNetty.newHttpServerBuilder(7999, new RequestHandler<ByteBuf, ByteBuf>() {
                    @Override
                    public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                   HttpServerResponse<ByteBuf> response) {
                        request.getContent().subscribe();
                        response.writeString("Hellooo!!!");
                        return response.close().finallyDo(new Action0() {
                            @Override
                            public void call() {
                                bothClientAndServerDone.countDown();
                            }
                        });
                    }
                }).appendPipelineConfigurator(new PipelineConfigurator<HttpServerRequest<ByteBuf>, HttpServerResponse<ByteBuf>>() {
                    @Override
                    public void configureNewPipeline(ChannelPipeline pipeline) {
                        pipeline.addLast(serverCloseListener);
                    }
                }).enableWireLogging(LogLevel.ERROR).build().start();
        TestableServerMetricsEventListener listener = new TestableServerMetricsEventListener();
        server.subscribe(listener);

        TestableClientMetricsEventListener clientListener = new TestableClientMetricsEventListener();
        final ChannelCloseListener clientCloseListener = new ChannelCloseListener();
        HttpClient<ByteBuf, ByteBuf> client =
                RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", server.getServerPort())
                       .enableWireLogging(LogLevel.DEBUG)
                       .appendPipelineConfigurator(new PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>>() {
                           @Override
                           public void configureNewPipeline(ChannelPipeline pipeline) {
                               pipeline.addLast(clientCloseListener);
                           }
                       })
                       .withNoConnectionPooling().build();
        client.subscribe(clientListener);

        client.submit(HttpClientRequest.createGet("/"))
              .finallyDo(new Action0() {
                  @Override
                  public void call() {
                      bothClientAndServerDone.countDown();
                  }
              }).toBlocking().last();

        bothClientAndServerDone.await(1, TimeUnit.MINUTES);
        clientCloseListener.waitForClose(1, TimeUnit.MINUTES);
        serverCloseListener.waitForClose(1, TimeUnit.MINUTES);

        Assert.assertTrue("Invalid TCP Server metric callbacks: " + listener.getEventTypeVsInvalidInvocations(),
                          listener.getEventTypeVsInvalidInvocations().isEmpty());
        Assert.assertTrue("Invalid HTTP Server metric callbacks: " + listener.getHttpEventTypeVsInvalidInvocations(),
                          listener.getHttpEventTypeVsInvalidInvocations().isEmpty());

        Assert.assertTrue("Invalid TCP client metric callbacks: " + clientListener.getEventTypeVsInvalidInvocations(),
                          clientListener.getEventTypeVsInvalidInvocations().isEmpty());
        Assert.assertTrue("Invalid HTTP client metric callbacks: " + clientListener.getHttpEventTypeVsInvalidInvocations(),
                          clientListener.getHttpEventTypeVsInvalidInvocations().isEmpty());

        testServerEventFired(listener, ServerMetricsEvent.EventType.NewClientConnected, 1);
        testServerEventFired(listener, ServerMetricsEvent.EventType.ConnectionHandlingStart, 1);
        testServerEventFired(listener, ServerMetricsEvent.EventType.ConnectionHandlingSuccess, 1);
        testServerEventFired(listener, ServerMetricsEvent.EventType.ConnectionCloseStart, 1);
        testServerEventFired(listener, ServerMetricsEvent.EventType.ConnectionCloseSuccess, 1);
        testServerEventFired(listener, ServerMetricsEvent.EventType.WriteStart);
        testServerEventFired(listener, ServerMetricsEvent.EventType.WriteSuccess);
        testServerEventFired(listener, ServerMetricsEvent.EventType.FlushStart, 2); // Auto-flush on request handling complete.
        testServerEventFired(listener, ServerMetricsEvent.EventType.FlushSuccess, 1);
        testServerEventFired(listener, ServerMetricsEvent.EventType.BytesRead);

        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.NewRequestReceived, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.RequestHandlingStart, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.RequestHeadersReceived, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.RequestReceiveComplete, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.ResponseHeadersWriteStart, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.ResponseHeadersWriteSuccess, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.ResponseContentWriteStart, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.ResponseContentWriteSuccess, 1);
        testHttpServerEventFired(listener, HttpServerMetricsEvent.EventType.RequestHandlingSuccess, 1);

        testClientEventFired(clientListener, ClientMetricsEvent.EventType.ConnectStart, 1);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.ConnectSuccess, 1);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.FlushStart, 1);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.FlushSuccess, 1);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.WriteStart);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.WriteSuccess);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.BytesRead);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.ConnectionCloseStart);
        testClientEventFired(clientListener, ClientMetricsEvent.EventType.ConnectionCloseSuccess);

        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.RequestSubmitted, 1);
        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.RequestHeadersWriteStart, 1);
        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.RequestHeadersWriteSuccess, 1);
        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.RequestWriteComplete, 1);
        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.ResponseHeadersReceived, 1);
        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.ResponseContentReceived, 1);
        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.ResponseReceiveComplete, 1);
        testHttpClientEventFired(clientListener, HttpClientMetricsEvent.EventType.RequestProcessingComplete, 1);
    }

    private static void testHttpClientEventFired(TestableClientMetricsEventListener listener,
                                                 HttpClientMetricsEvent.EventType eventType, int count) {
        Assert.assertTrue("Client event type: " + eventType + " not fired",
                          listener.getHttpEeventTypeVsInvocations().containsKey(eventType));
        if (-1 != count) {
            Assert.assertEquals("Client event type: " + eventType + " not fired as many times as expected.",
                                count, (long) listener.getHttpEeventTypeVsInvocations().get(eventType));
        }
    }

    private static void testClientEventFired(TestableClientMetricsEventListener listener,
                                             ClientMetricsEvent.EventType eventType) {
        testClientEventFired(listener, eventType, -1);
    }

    private static void testClientEventFired(TestableClientMetricsEventListener listener,
                                             ClientMetricsEvent.EventType eventType, int count) {
        Assert.assertTrue("Client event type: " + eventType + " not fired",
                          listener.getEventTypeVsInvocations().containsKey(eventType));
        if (-1 != count) {
            Assert.assertEquals("Client event type: " + eventType + " not fired as many times as expected.",
                                count, (long) listener.getEventTypeVsInvocations().get(eventType));
        }
    }

    private static void testHttpServerEventFired(TestableServerMetricsEventListener listener,
                                                 HttpServerMetricsEvent.EventType eventType, int count) {
        Assert.assertTrue("Server event type: " + eventType + " not fired",
                          listener.getHttpEeventTypeVsInvocations().containsKey(eventType));
        if (-1 != count) {
            Assert.assertEquals("Server event type: " + eventType + " not fired as many times as expected.",
                                count, (long) listener.getHttpEeventTypeVsInvocations().get(eventType));
        }
    }

    private static void testServerEventFired(TestableServerMetricsEventListener listener,
                                             ServerMetricsEvent.EventType eventType) {
        testServerEventFired(listener, eventType, -1);
    }

    private static void testServerEventFired(TestableServerMetricsEventListener listener,
                                             ServerMetricsEvent.EventType eventType, int count) {
        Assert.assertTrue("Server event type: " + eventType + " not fired",
                          listener.getEventTypeVsInvocations().containsKey(eventType));
        if (-1 != count) {
            Assert.assertEquals("Server event type: " + eventType + " not fired as many times as expected.",
                                count, (long) listener.getEventTypeVsInvocations().get(eventType));
        }
    }
}
