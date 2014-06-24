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
package io.reactivex.netty.servo.tcp;

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.ClientBuilder;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.servo.ServoEventsListenerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class TcpMetricsTest {

    private ServoEventsListenerFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new ServoEventsListenerFactory("tcp-metric-test-client", "tcp-metric-test-server");
    }

    @Test
    public void testClientMetrics() throws Exception {
        ClientBuilder<Object, Object> builder = RxNetty.newTcpClientBuilder("localhost", 9999);
        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = builder.getEventsSubject();

        RxClient<Object, Object> client = builder.build();
        TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> listener = factory.forTcpClient(client);
        client.subscribe(listener);

        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
        Assert.assertEquals("Invalid pending connect count.", 1, listener.getPendingConnects());

        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid pending connect count after connect success.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid live connect count after connect success.", 1, listener.getLiveConnections());
        Assert.assertEquals("Invalid connect time.", 1, (long)listener.getConnectionTimes().getValue());

        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_START);
        eventsSubject.onEvent(ClientMetricsEvent.CONNECTION_CLOSE_SUCCESS, 1, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Invalid live connect count after connection close.", 0, listener.getLiveConnections());

        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
        Assert.assertEquals("Invalid pending connect count.", 1, listener.getPendingConnects());

        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_FAILED, 1, TimeUnit.MILLISECONDS, new IllegalStateException());
        Assert.assertEquals("Invalid pending connect count after connect failure.", 0, listener.getPendingConnects());
        Assert.assertEquals("Invalid live connect count after connect failure.", 0, listener.getLiveConnections());
        Assert.assertEquals("Invalid failed connect count after connect failure.", 1, listener.getFailedConnects());
        Assert.assertEquals("Invalid connect time after failed connect.", 1, (long)listener.getConnectionTimes().getValue());
    }
}
