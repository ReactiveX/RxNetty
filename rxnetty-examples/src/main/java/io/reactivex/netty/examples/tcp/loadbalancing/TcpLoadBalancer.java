/*
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.netty.examples.tcp.loadbalancing;

import io.reactivex.netty.protocol.tcp.client.ConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ConnectionProvider;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class TcpLoadBalancer<W, R> extends RoundRobinLoadBalancer<W, R> {

    private TcpLoadBalancer(SocketAddress[] hosts, ConnectionFactory<W, R> connectionFactory) {
        super(hosts, connectionFactory, removeAction -> new TcpClientEventListener() {
            @Override
            public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
                removeAction.call();
            }
        });
    }

    public static <W, R> ConnectionProvider<W, R> create(SocketAddress[] hosts) {
        return ConnectionProvider.create(connectionFactory -> new TcpLoadBalancer<W, R>(hosts, connectionFactory));
    }
}
