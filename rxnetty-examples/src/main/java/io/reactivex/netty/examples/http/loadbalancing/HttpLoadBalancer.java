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
package io.reactivex.netty.examples.http.loadbalancing;

import io.reactivex.netty.examples.tcp.loadbalancing.RoundRobinLoadBalancer;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.protocol.tcp.client.ConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ConnectionProvider;

import java.net.SocketAddress;

/**
 * This is an implementation of {@link RoundRobinLoadBalancer} for HTTP clients.
 *
 * This load balancer uses a naive failure detector that removes the host on the first 503 HTTP response. The
 * intention here is just to demonstrate how to write complex load balancing logic using lower level constructs in the
 * client.
 *
 * @param <W> Type of Objects written on the connections created by this load balancer.
 * @param <R> Type of Objects read from the connections created by this load balancer.
 *
 * @see RoundRobinLoadBalancer
 * @see HttpLoadBalancingClient
 */
public class HttpLoadBalancer<W, R> extends RoundRobinLoadBalancer<W, R> {

    private HttpLoadBalancer(SocketAddress[] hosts, ConnectionFactory<W, R> connectionFactory) {
        super(hosts, connectionFactory, removeAction -> new HttpClientEventsListener() {
            @Override
            public void onResponseHeadersReceived(int responseCode) {
                if (503 == responseCode) {
                    /*Remove the host from the active list, if we get a 503 response*/
                    removeAction.call();
                }
            }
        });
    }

    /**
     * Creates a new instance of {@link HttpLoadBalancer} load balancing on the passed array of hosts.
     *
     * @param hosts Array of hosts to load balance on.
     *
     * @return A new {@link ConnectionProvider} that creates instances of {@link HttpLoadBalancer}
     */
    public static <W, R> ConnectionProvider<W, R> create(SocketAddress[] hosts) {
        return ConnectionProvider.create(connectionFactory -> new HttpLoadBalancer<W, R>(hosts, connectionFactory));
    }
}
