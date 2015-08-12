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
 *
 */
package io.reactivex.netty.examples.tcp.loadbalancing;

import io.reactivex.netty.client.ConnectionFactory;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import rx.Observable;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * This is an implementation of {@link RoundRobinLoadBalancer} for TCP clients.
 *
 * This load balancer uses a naive failure detector that removes the host on the first connection failure. The
 * intention here is just to demonstrate how to write complex load balancing logic using lower level constructs in the
 * client.
 *
 * @param <W> Type of Objects written on the connections created by this load balancer.
 * @param <R> Type of Objects read from the connections created by this load balancer.
 */
public class TcpLoadBalancer<W, R> extends RoundRobinLoadBalancer<W, R> {

    private TcpLoadBalancer(Observable<SocketAddress> hosts, ConnectionFactory<W, R> connectionFactory) {
        super(hosts, connectionFactory, removeAction -> new TcpClientEventListener() {
            @Override
            public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
                /*Remove the host from the active list, if a connect fails*/
                removeAction.call();
            }
        });
    }

    /**
     * Creates a new instance of {@link TcpLoadBalancer} load balancing on the passed hosts.
     *
     * @param hosts Hosts to load balance on.
     *
     * @return A new {@link ConnectionProvider} that creates instances of {@link TcpLoadBalancer}
     */
    public static <W, R> ConnectionProvider<W, R> create(Observable<SocketAddress> hosts) {
        return ConnectionProvider.create(connectionFactory -> new TcpLoadBalancer<>(hosts, connectionFactory));
    }
}
