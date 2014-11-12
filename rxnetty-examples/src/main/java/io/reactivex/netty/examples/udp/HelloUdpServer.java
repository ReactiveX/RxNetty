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
package io.reactivex.netty.examples.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.udp.server.UdpServer;
import rx.Observable;
import rx.functions.Func1;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public final class HelloUdpServer {

    static final int DEFAULT_PORT = 8098;
    static final String WELCOME_MSG = "Welcome to the broadcast world!";
    static final byte[] WELCOME_MSG_BYTES = WELCOME_MSG.getBytes(Charset.defaultCharset());

    public final int port;

    public HelloUdpServer(int port) {
        this.port = port;
    }

    public UdpServer<DatagramPacket, DatagramPacket> createServer() {
        UdpServer<DatagramPacket, DatagramPacket> server = RxNetty.createUdpServer(port, new ConnectionHandler<DatagramPacket, DatagramPacket>() {
            @Override
            public Observable<Void> handle(final ObservableConnection<DatagramPacket, DatagramPacket> newConnection) {
                return newConnection.getInput().flatMap(new Func1<DatagramPacket, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(DatagramPacket received) {
                        InetSocketAddress sender = received.sender();
                        System.out.println("Received datagram. Sender: " + sender + ", data: "
                                + received.content().toString(Charset.defaultCharset()));
                        ByteBuf data = newConnection.getChannel().alloc().buffer(WELCOME_MSG_BYTES.length);
                        data.writeBytes(WELCOME_MSG_BYTES);
                        return newConnection.writeAndFlush(new DatagramPacket(data, sender));
                    }
                });
            }
        });
        System.out.println("UDP hello server started...");
        return server;
    }

    public static void main(String[] args) {
        new HelloUdpServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
