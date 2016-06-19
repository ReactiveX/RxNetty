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

package io.reactivex.netty.examples.local;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.client.ChannelProvider;
import io.reactivex.netty.client.ChannelProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.threads.RxDefaultThreadFactory;
import org.slf4j.Logger;
import rx.Observable;

import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * A local transport "Hello World" example that sends a string "Hello World!" to a local transport server and expects
 * an echo response.
 */
public final class LocalEcho {

    public static void main(String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(LocalEcho.class);
        Logger logger = env.getLogger();

        LocalAddress serverAddress = new LocalAddress("local-example");

        /*Starts a new Local transport server.*/
        DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(0, new RxDefaultThreadFactory("local-server"));
        TcpServer.newServer(serverAddress,
                            eventLoopGroup,
                            LocalServerChannel.class)
                 .enableWireLogging("echo-server", LogLevel.DEBUG)
                 .start(connection -> connection
                         .writeStringAndFlushOnEach(connection.getInput()
                                                              .map(bb -> bb.toString(Charset.defaultCharset()))
                                                              .map(msg -> "echo => " + msg)));

        /*Create a new client for the server address*/
        TcpClient.newClient(serverAddress, eventLoopGroup, LocalChannel.class)
                 .enableWireLogging("echo-client", LogLevel.DEBUG)
                 .createConnectionRequest()
                 .flatMap(connection ->
                                  connection.writeString(Observable.just("Hello World!"))
                                            .cast(ByteBuf.class)
                                            .concatWith(connection.getInput())
                 )
                 .take(1)
                 .map(bb -> bb.toString(Charset.defaultCharset()))
                 .toBlocking()
                 .forEach(logger::info);
    }
}
