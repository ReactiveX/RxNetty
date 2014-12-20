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

package io.reactivex.netty.spectator.http;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.http.client.HttpClientMetricsEvent;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerMetricsEvent;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.ServerMetricsEvent;
import io.reactivex.netty.spectator.SpectatorEventsListenerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class HttpMetricsTest {

    private SpectatorEventsListenerFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new SpectatorEventsListenerFactory("http-metric-test-client", "http-metric-test-server");
    }

    @Test
    public void testServerMetrics() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return Observable.empty();
            }
        });
        MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject = server.getEventsSubject();

        HttpServerListener listener = factory.forHttpServer(server);
        server.subscribe(listener);
        long responseTimes = listener.getResponseWriteTimes().totalTime();

        doRequestStart(eventsSubject, listener);
        Assert.assertEquals("Unexpected live connections.", 1, listener.getLiveConnections());

        eventsSubject.onEvent(HttpServerMetricsEvent.RESPONSE_HEADERS_WRITE_START);
        eventsSubject.onEvent(HttpServerMetricsEvent.RESPONSE_HEADERS_WRITE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HANDLING_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected inflight requests.", 0, listener.getInflightRequests());
        Assert.assertEquals("Unexpected response write times.", TimeUnit.MILLISECONDS.toNanos(1) + responseTimes,
                listener.getResponseWriteTimes().totalTime());
        Assert.assertEquals("Unexpected processed requests.", 1, listener.getProcessedRequests());

        doRequestStart(eventsSubject, listener);
        Assert.assertEquals("Unexpected live connections.", 2, listener.getLiveConnections());

        responseTimes = listener.getResponseWriteTimes().totalTime();
        eventsSubject.onEvent(HttpServerMetricsEvent.RESPONSE_HEADERS_WRITE_START);
        eventsSubject.onEvent(HttpServerMetricsEvent.RESPONSE_HEADERS_WRITE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HANDLING_FAILED, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected inflight requests.", 0, listener.getInflightRequests());
        Assert.assertEquals("Unexpected response write times.", responseTimes + TimeUnit.MILLISECONDS.toNanos(1),
                listener.getResponseWriteTimes().totalTime());
        Assert.assertEquals("Unexpected processed requests.", 2, listener.getProcessedRequests());
        Assert.assertEquals("Unexpected failed requests.", 1, listener.getFailedRequests());
    }

    @Test
    public void testClientMetrics() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> builder = RxNetty.newHttpClientBuilder("localhost", 0);
        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = builder.getEventsSubject();
        HttpClient<ByteBuf, ByteBuf> client = builder.build();
        HttpClientListener listener = factory.forHttpClient(client);
        client.subscribe(listener);

        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_SUBMITTED);
        Assert.assertEquals("Unexpected request backlog.", 1, listener.getRequestBacklog());

        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
        Assert.assertEquals("Invalid pending connect count.", 1, listener.getPendingConnects());
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending connect count after connect success.", 0, listener.getPendingConnects());

        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_HEADERS_WRITE_START);
        Assert.assertEquals("Unexpected request backlog.", 0, listener.getRequestBacklog());
        Assert.assertEquals("Unexpected inflight requests.", 1, listener.getInflightRequests());

        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_HEADERS_WRITE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_WRITE_COMPLETE, 1, TimeUnit.MILLISECONDS);
        eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_RECEIVE_COMPLETE, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected request backlog.", 0, listener.getInflightRequests());
        Assert.assertEquals("Unexpected processed request.", 1, listener.getProcessedRequests());

        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_SUBMITTED);
        Assert.assertEquals("Unexpected request backlog.", 1, listener.getRequestBacklog());

        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_HEADERS_WRITE_START);
        Assert.assertEquals("Unexpected request backlog.", 0, listener.getRequestBacklog());
        Assert.assertEquals("Unexpected inflight requests.", 1, listener.getInflightRequests());

        eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_FAILED, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected inflight requests.", 0, listener.getInflightRequests());
        Assert.assertEquals("Unexpected processed request.", 2, listener.getProcessedRequests());
        Assert.assertEquals("Unexpected failed requests.", 1, listener.getFailedResponses());

    }

    private static void doRequestStart(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject,
                                       HttpServerListener listener) {
        long reqReadTimes = listener.getRequestReadTimes().totalTime();
        eventsSubject.onEvent(ServerMetricsEvent.NEW_CLIENT_CONNECTED);
        eventsSubject.onEvent(HttpServerMetricsEvent.NEW_REQUEST_RECEIVED);
        Assert.assertEquals("Unexpected request backlog.", 1, listener.getRequestBacklog());
        eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HANDLING_START, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected inflight requests.", 1, listener.getInflightRequests());
        Assert.assertEquals("Unexpected request backlog..", 0, listener.getRequestBacklog());
        eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_RECEIVE_COMPLETE, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected inflight requests.", reqReadTimes + TimeUnit.MILLISECONDS.toNanos(1),
                listener.getRequestReadTimes().totalTime());
    }
}
