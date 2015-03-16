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
package io.reactivex.netty.examples.tcp.serverpool;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.string.StringEncoder;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ServerPool;
import io.reactivex.netty.client.ServerPool.Server;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServerImpl;
import rx.Observable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nitesh Kant
 */
public final class ServerPoolClient {

    public static void main(String[] args) {

        final List<Server<ClientMetricsEvent<?>>> servers = new ArrayList<>();

        servers.add(new ServerImpl(new InetSocketAddress("127.0.0.1", 8099)));
        servers.add(new ServerImpl(new InetSocketAddress("127.0.0.1", 8098)));
        servers.add(new ServerImpl(new InetSocketAddress("127.0.0.1", 8097)));

        for (Server<ClientMetricsEvent<?>> server : servers) {
            int port = ((InetSocketAddress) server.getAddress()).getPort();
            new TcpServerImpl<ByteBuf, ByteBuf>(port)
                    .<ByteBuf, String>addChannelHandlerLast("encoder", StringEncoder::new)
                    .start(conn -> conn.writeAndFlush("Hello from port: " + port));
        }

        final ServerPool<ClientMetricsEvent<?>> pool = new ServerPool<ClientMetricsEvent<?>>() {

            final AtomicInteger nextCounter = new AtomicInteger();
            final int size = servers.size();

            @Override
            public Server<ClientMetricsEvent<?>> next() {
                int nextIndex = Math.abs(nextCounter.incrementAndGet() % size);
                return servers.get(nextIndex);
            }
        };

        TcpClient.newClient(pool)
                 .createConnectionRequest()
                 .switchMap(Connection::getInput)
                 .repeat(10)
                 .toBlocking()
                 .forEach(bb -> System.out.println(bb.toString(Charset.defaultCharset())));
    }

    private static final class ServerImpl implements Server<ClientMetricsEvent<?>> {

        private final SocketAddress address;

        private ServerImpl(SocketAddress address) {
            this.address = address;
        }

        @Override
        public SocketAddress getAddress() {
            return address;
        }

        @Override
        public Observable<Void> getLifecycle() {
            return Observable.never();
        }

        @Override
        public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                            Object value) {
            // No op
        }

        @Override
        public void onCompleted() {
            // No op
        }

        @Override
        public void onSubscribe() {
            // No op
        }
    }
}
