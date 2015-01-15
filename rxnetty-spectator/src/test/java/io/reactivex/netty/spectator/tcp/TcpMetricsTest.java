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
package io.reactivex.netty.spectator.tcp;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientBuilder;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.server.RxServer;
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
public class TcpMetricsTest {

    private SpectatorEventsListenerFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new SpectatorEventsListenerFactory("tcp-metric-test-client", "tcp-metric-test-server");
    }

    @Test
    public void testServerMetrics() throws Exception {
        RxServer<ByteBuf, ByteBuf> server = RxNetty.createTcpServer(0, new ConnectionHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(ObservableConnection<ByteBuf, ByteBuf> newConnection) {
                return Observable.empty();
            }
        });
        MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject = server.getEventsSubject();

        TcpServerListener<ServerMetricsEvent<ServerMetricsEvent.EventType>> listener = factory.forTcpServer(server);
        server.subscribe(listener);

        eventsSubject.onEvent(ServerMetricsEvent.NEW_CLIENT_CONNECTED);
        Assert.assertEquals("Unexpected live connections.", 1, listener.getLiveConnections());

        eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_HANDLING_START);
        Assert.assertEquals("Unexpected live connections.", 1, listener.getLiveConnections());
        Assert.assertEquals("Unexpected inflight connections.", 1, listener.getInflightConnections());

        eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_HANDLING_SUCCESS , 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected live connections.", 1, listener.getLiveConnections());
        Assert.assertEquals("Unexpected inflight connections.", 0, listener.getInflightConnections());

        eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_CLOSE_START);
        Assert.assertEquals("Unexpected live connections.", 1, listener.getLiveConnections());
        Assert.assertEquals("Unexpected inflight connections.", 0, listener.getInflightConnections());
        eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_CLOSE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Unexpected live connections.", 0, listener.getLiveConnections());
        Assert.assertEquals("Unexpected inflight connections.", 0, listener.getInflightConnections());

        eventsSubject.onEvent(ServerMetricsEvent.WRITE_START, (Object) 1000);
        Assert.assertEquals("Invalid pending flush count.", 1, listener.getPendingWrites());
        eventsSubject.onEvent(ServerMetricsEvent.WRITE_SUCCESS, 1, TimeUnit.MILLISECONDS, 1000);
        Assert.assertEquals("Invalid write time metric.", TimeUnit.MILLISECONDS.toNanos(1), listener.getWriteTimes().totalTime());
        Assert.assertEquals("Invalid bytes written metric.", 1000, listener.getBytesWritten());

        eventsSubject.onEvent(ServerMetricsEvent.FLUSH_START, (Object) 1000);
        Assert.assertEquals("Invalid pending flush count.", 1, listener.getPendingFlushes());
        eventsSubject.onEvent(ServerMetricsEvent.FLUSH_SUCCESS, 1, TimeUnit.MILLISECONDS, 1000);
        Assert.assertEquals("Invalid flush metric.", TimeUnit.MILLISECONDS.toNanos(1), listener.getFlushTimes().totalTime());

        eventsSubject.onEvent(ServerMetricsEvent.FLUSH_START, (Object) 1000);
        Assert.assertEquals("Invalid pending flush count.", 1, listener.getPendingFlushes());
        eventsSubject.onEvent(ServerMetricsEvent.FLUSH_FAILED, 1, TimeUnit.MILLISECONDS, 1000);
        Assert.assertEquals("Invalid pending flush count.", 1, listener.getFailedFlushes());
        Assert.assertEquals("Invalid flush metric.", TimeUnit.MILLISECONDS.toNanos(1), listener.getFlushTimes().totalTime());

        eventsSubject.onEvent(ServerMetricsEvent.BYTES_READ, (Object) 1000);
        Assert.assertEquals("Invalid bytes read.", 1000, listener.getBytesRead());

    }

    @Test
    public void testClientMetrics() throws Exception {
        ClientBuilder<Object, Object> builder = RxNetty.newTcpClientBuilder("localhost", 9999);
        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = builder.getEventsSubject();

        RxClient<Object, Object> client = builder.build();
        TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener = factory.forTcpClient(client);
        client.subscribe(listener);

        doConnectSuccess(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after connect success.", 1, listener.getConnectionCount());

        doConnectCloseSuccess(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after  connection close.", 1, listener.getConnectionCount());

        doConnectFailed(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after connect failure.", 1, listener.getConnectionCount());

        doConnectSuccess(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after connect success.", 2, listener.getConnectionCount());

        doConnCloseFailed(eventsSubject, listener);

        doPoolAcquireSuccess(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after connect success.", 3, listener.getConnectionCount());
        Assert.assertEquals("Invalid pool acquire count.", 1, listener.getPoolAcquires());

        doPoolReuse(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after connect reuse.", 3, listener.getConnectionCount());
        Assert.assertEquals("Invalid pool acquire count.", 2, listener.getPoolAcquires());

        doEviction(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after eviction.", 3, listener.getConnectionCount());

        doPoolAcquireSuccess(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after connect success.", 4, listener.getConnectionCount());
        Assert.assertEquals("Invalid pool acquire count.", 3, listener.getPoolAcquires());

        doPoolReleaseSuccess(eventsSubject, listener);
        Assert.assertEquals("Invalid total connections count after connect success.", 4, listener.getConnectionCount());

        doWrite(eventsSubject, listener);

        doFlush(eventsSubject, listener);

        doRead(eventsSubject, listener);
    }

    private static void doRead(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                               TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        eventsSubject.onEvent(ClientMetricsEvent.BYTES_READ, (Object) 1000);
        Assert.assertEquals("Invalid bytes read metric.", 1000, listener.getBytesRead());
    }

    private static void doFlush(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        long flushTimes = listener.getFlushTimes().totalTime();
        eventsSubject.onEvent(ClientMetricsEvent.FLUSH_START, (Object) 1000);
        Assert.assertEquals("Invalid pending flush count.", 1, listener.getPendingFlushes());
        eventsSubject.onEvent(ClientMetricsEvent.FLUSH_SUCCESS, 1, TimeUnit.MILLISECONDS, 1000);

        Assert.assertEquals("Invalid flush metric.", flushTimes + TimeUnit.MILLISECONDS.toNanos(1),
                listener.getFlushTimes().totalTime());
        Assert.assertEquals("Invalid bytes written metric.", 1000, listener.getBytesWritten());
    }

    private static void doWrite(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        long writeTimes = listener.getWriteTimes().totalTime();
        eventsSubject.onEvent(ClientMetricsEvent.WRITE_START, (Object) 1000);
        Assert.assertEquals("Invalid pending flush count.", 1, listener.getPendingWrites());
        eventsSubject.onEvent(ClientMetricsEvent.WRITE_SUCCESS, 1, TimeUnit.MILLISECONDS, 1000);

        Assert.assertEquals("Invalid write time metric.", writeTimes + TimeUnit.MILLISECONDS.toNanos(1),
                listener.getWriteTimes().totalTime());
        Assert.assertEquals("Invalid bytes written metric.", 1000, listener.getBytesWritten());
    }

    private static void doPoolReleaseSuccess(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                             TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        eventsSubject.onEvent(ClientMetricsEvent.POOL_RELEASE_START);
        eventsSubject.onEvent(ClientMetricsEvent.POOL_RELEASE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid live connect count.", 1, listener.getLiveConnections());
        Assert.assertEquals("Invalid pending connect count.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid pool acquire count.", 1, listener.getPoolReleases());
    }

    private static void doConnectFailed(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                        TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        long connectionTimes = listener.getConnectionTimes().totalTime();
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
        Assert.assertEquals("Invalid pending connect count.", 1, listener.getPendingConnects());
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_FAILED, 1, TimeUnit.MILLISECONDS, new IllegalStateException());
        Assert.assertEquals("Invalid pending connect count after connect failure.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid live connect count after connect failure.", 0, listener.getLiveConnections());
        Assert.assertEquals("Invalid failed connect count after connect failure.", 1, listener.getFailedConnects());
        Assert.assertEquals("Invalid connect time after failed connect.",
                connectionTimes, listener.getConnectionTimes().totalTime());
    }

    private static void doConnectCloseSuccess(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                              TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_START);
        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid live connect count after connection close.", 0, listener.getLiveConnections());
        Assert.assertEquals("Invalid pending connect count after connection close.", 0, listener.getPendingConnects());
    }

    private static void doConnectSuccess(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                         TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        long totalTime = listener.getConnectionTimes().totalTime();
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
        Assert.assertEquals("Invalid pending connect count.", 1, listener.getPendingConnects());
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending connect count after connect success.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid live connect count after connect success.", 1, listener.getLiveConnections());
        Assert.assertEquals("Invalid connect time.", totalTime + TimeUnit.MILLISECONDS.toNanos(1),
                listener.getConnectionTimes().totalTime());
    }

    private static void doConnCloseFailed(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                          TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_START);
        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_FAILED, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending connect count after connection close.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid live connect count after connection close.", 0, listener.getLiveConnections());
        Assert.assertEquals("Invalid live connect count after connection close.", 1, listener.getFailedConnectionClose());
    }

    private static void doEviction(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                   TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        eventsSubject.onEvent(ClientMetricsEvent.POOLED_CONNECTION_EVICTION);
        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_START); // Eviction does not mean a connection close, this event is explicit.
        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_SUCCESS);
        Assert.assertEquals("Invalid live connect count after eviction.", 0, listener.getLiveConnections());
        Assert.assertEquals("Invalid pool eviction count after eviction.", 1, listener.getPoolEvictions());
    }

    private static void doPoolReuse(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                    TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        eventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_START);
        Assert.assertEquals("Invalid pending pool acquire count.", 1, listener.getPendingPoolAcquires());
        eventsSubject.onEvent(ClientMetricsEvent.POOLED_CONNECTION_REUSE, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending connect count after connection reuse.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid live connect count after connect reuse.", 1, listener.getLiveConnections());
        Assert.assertEquals("Invalid pool reuse count after connect reuse.", 1, listener.getPoolReuse());
        eventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending pool acquire count.", 0, listener.getPendingPoolAcquires());
    }

    private static void doPoolAcquireSuccess(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                             TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener) {
        long poolTime = listener.getPoolAcquireTimes().totalTime();
        eventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_START);
        Assert.assertEquals("Invalid pending pool acquire count.", 1, listener.getPendingPoolAcquires());
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending connect count after connection success.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid live connect count after connect success.", 1, listener.getLiveConnections());

        eventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending connect count after connection success.", 0,
                listener.getPendingPoolAcquires());
        Assert.assertEquals("Invalid pool acquire times after connection success.",
                poolTime + TimeUnit.MILLISECONDS.toNanos(1), listener.getPoolAcquireTimes().totalTime());
    }
}
