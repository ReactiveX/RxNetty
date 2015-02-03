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

package io.reactivex.netty.examples.tcp.echo;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.string.StringEncoder;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.protocol.tcp.server.TcpServerImpl;
import io.reactivex.netty.protocol.text.StringLineDecoder;

/**
 * @author Nitesh Kant
 */
public final class TcpEchoServer {

    static final int DEFAULT_PORT = 8099;

    private final int port;

    public TcpEchoServer(int port) {
        this.port = port;
    }

    public TcpServer<String, String> startServer() {
        TcpServer<String, String> server = new TcpServerImpl<ByteBuf, ByteBuf>(port)
                .<ByteBuf, String>addChannelHandlerLast("encoder", StringEncoder::new)
                .<String, String>addChannelHandlerLast("decoder", StringLineDecoder::new)
                .start(connection -> connection.writeAndFlushOnEach(connection.getInput()
                                                                              .map(msg -> "echo => " + msg + '\n')));

        return server;
    }

    public static void main(final String[] args) {
        new TcpEchoServer(DEFAULT_PORT).startServer().waitTillShutdown();
    }
}
