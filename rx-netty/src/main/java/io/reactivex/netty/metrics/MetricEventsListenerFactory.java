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

import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.websocket.WebSocketClient;
import io.reactivex.netty.protocol.http.websocket.WebSocketServer;
import io.reactivex.netty.protocol.udp.client.UdpClient;
import io.reactivex.netty.protocol.udp.server.UdpServer;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * A factory to create new {@link MetricEventsListener}. This is used if there is a need to set the system wide
 * {@link MetricEventsListener} for all clients and servers, instead of specifying them per client/server instance.
 *
 * @author Nitesh Kant
 */
public abstract class MetricEventsListenerFactory {

    public abstract MetricEventsListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> forTcpClient(
            @SuppressWarnings("rawtypes") RxClient client);

    public abstract MetricEventsListener<ClientMetricsEvent<?>> forHttpClient(@SuppressWarnings("rawtypes")HttpClient client);

    public abstract MetricEventsListener<ClientMetricsEvent<?>> forWebSocketClient(@SuppressWarnings("rawtypes")WebSocketClient client);

    public abstract MetricEventsListener<ClientMetricsEvent<?>> forUdpClient(@SuppressWarnings("rawtypes")UdpClient client);

    public abstract MetricEventsListener<ServerMetricsEvent<ServerMetricsEvent.EventType>> forTcpServer(
            @SuppressWarnings("rawtypes") RxServer server);

    public abstract MetricEventsListener<ServerMetricsEvent<?>> forHttpServer(@SuppressWarnings("rawtypes")HttpServer server);

    public abstract MetricEventsListener<ServerMetricsEvent<?>> forWebSocketServer(@SuppressWarnings("rawtypes")WebSocketServer server);

    public abstract MetricEventsListener<ServerMetricsEvent<?>> forUdpServer(@SuppressWarnings("rawtypes")UdpServer server);
}
