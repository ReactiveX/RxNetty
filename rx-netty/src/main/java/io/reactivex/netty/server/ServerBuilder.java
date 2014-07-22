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

package io.reactivex.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;

/**
 * A convenience builder for creating instances of {@link RxServer}
 *
 * @author Nitesh Kant
 */
public class ServerBuilder<I, O> extends ConnectionBasedServerBuilder<I,O, ServerBuilder<I, O>> {

    public ServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        super(port, connectionHandler);
    }

    public ServerBuilder(int port, ConnectionHandler<I, O> connectionHandler, ServerBootstrap bootstrap) {
        super(port, connectionHandler, bootstrap);
    }

    @Override
    protected RxServer<I, O> createServer() {
        return new RxServer<I, O>(serverBootstrap, port, pipelineConfigurator, connectionHandler, eventExecutorGroup);
    }

    @Override
    protected MetricEventsListener<ServerMetricsEvent<ServerMetricsEvent.EventType>>
    newMetricsListener(MetricEventsListenerFactory factory, RxServer<I, O> server) {
        return factory.forTcpServer(server);
    }
}
