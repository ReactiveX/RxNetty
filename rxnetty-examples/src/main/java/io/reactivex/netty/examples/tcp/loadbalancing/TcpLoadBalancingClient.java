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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import rx.Observable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * This example demonstrates how to integrate any arbitrary load balancing logic with a {@link TcpClient}. Load
 * balancing algorithms are not provided by {@code RxNetty}, what is provided is a low level construct of
 * {@link ConnectionProvider} that abstracts providing connections for a {@link TcpClient}. Higher level constructs like
 * Load Balancing, connection pooling, etc. can be built using these building blocks.
 *
 * The code here uses a naive {@link TcpLoadBalancer} that removes a host on any connection failure and otherwise round
 * robins on the set of available hosts.
 *
 * This example, starts a couple emebedded TCP server and uses a list of these server addresses and an unavailable
 * server address to demonstrate failure detetction (not using the unavailable server) and round-robin load balancing
 * (alternating between the two available hosts for the requests)
 *
 * @see ConnectionProvider Low level abstraction to create varied load balancing schemes.
 * @see RoundRobinLoadBalancer An example of load balancer used by this example.
 */
public final class TcpLoadBalancingClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*Start two embedded servers and use there addresses as two hosts, add a unavailable server to demonstrate
        * failure detection.*/
        final Observable<SocketAddress> hosts = Observable.just(startNewServer(), startNewServer(),
                                                                new InetSocketAddress(0));

        /*Create a new client using the load balancer over the hosts above.*/
        TcpClient.<ByteBuf, ByteBuf>newClient(TcpLoadBalancer.create(hosts))
                /*Create a new connection request, each subscription creates a new connection*/
                 .createConnectionRequest()
                /*Log which host is being used, to show load balancing b/w hosts*/
                 .doOnNext(conn -> logger.info("Using host: " + conn.unsafeNettyChannel().remoteAddress()))
                /*Convert the stream from a connection to the input, post writing the "Hello World!" message*/
                 .flatMap(connection ->
                                  /*Write the message*/
                                  connection.writeString(Observable.just("Hello World!"))
                                          /*Since, write returns a Void stream, cast it to ByteBuf to be able to merge
                                          with the input*/
                                            .cast(ByteBuf.class)
                                          /*Upon successful completion of the write, subscribe to the connection input*/
                                            .concatWith(connection.getInput())
                 )
                /*Since, we only wrote a single message, we expect a single echo message back*/
                 .take(1)
                /*Convert each ByteBuf to a string*/
                 .map(bb -> bb.toString(Charset.defaultCharset()))
                /*Repeat the connect-write-read sequence five times to demonstrate load balancing on different requests*/
                 .repeat(5)
                /*Block till the response comes to avoid JVM exit.*/
                 .toBlocking()
                /*Print each content chunk*/
                 .forEach(logger::info);
    }

    private static SocketAddress startNewServer() {
        /*Start a new server on an ephemeral port that echoes all messages, as is.*/
        return TcpServer.newServer()
                        .start(conn -> conn.writeAndFlushOnEach(conn.getInput()))
                        .getServerAddress();
    }
}
