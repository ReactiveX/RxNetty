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

package io.reactivex.netty.examples.tcp.event;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.protocol.tcp.server.TcpServerImpl;
import rx.Observable;

/**
 * @author Nitesh Kant
 */
public final class TcpEventStreamServer {

    static final int DEFAULT_PORT = 8100;

    private final int port;

    public TcpEventStreamServer(int port) {
        this.port = port;
    }

    public TcpServer<String, String> startServer() {
        TcpServer<String, String> server = new TcpServerImpl<ByteBuf, ByteBuf>(port)
                .<ByteBuf, String>addChannelHandlerLast("encoder", StringEncoder::new)
                .<String, String>addChannelHandlerLast("decoder", StringDecoder::new)
                .clientChannelOption(ChannelOption.SO_SNDBUF, 100)
                .enableWireLogging(LogLevel.ERROR)
                .start(connection -> connection.writeAndFlushOnEach(Observable.range(1, 10000)
                                                                              .map(aLong -> "data: {\"requestCount\":"
                                                                                            + aLong + "}\n")));

        return server;
    }

    public static void main(String[] args) {
        new TcpEventStreamServer(DEFAULT_PORT).startServer().waitTillShutdown();
    }
}
