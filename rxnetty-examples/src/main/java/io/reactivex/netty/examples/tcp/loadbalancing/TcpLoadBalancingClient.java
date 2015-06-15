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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import rx.Observable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * An example to demonstrate how to do load balancing with TCP clients
 */
public final class TcpLoadBalancingClient extends AbstractClientExample {

    public static void main(String[] args) {

        final SocketAddress[] hosts = { startNewServer(), new InetSocketAddress(0)};

        TcpClient.<ByteBuf, ByteBuf>newClient(TcpLoadBalancer.create(hosts))
                 .createConnectionRequest()
                 .flatMap(connection ->
                                  connection.writeString(Observable.just("Hello World!"))
                                            .ignoreElements()
                                            .cast(ByteBuf.class)
                                            .mergeWith(connection.getInput())
                 )
                 .take(1)
                 .map(bb -> bb.toString(Charset.defaultCharset()))
                 .repeat(5)
                 .toBlocking()
                 .forEach(logger::info);
    }

    private static SocketAddress startNewServer() {
        return TcpServer.newServer()
                        .start(conn -> conn.writeAndFlushOnEach(conn.getInput()))
                        .getServerAddress();
    }
}
