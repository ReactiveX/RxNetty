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
package io.reactivex.netty.examples.tcp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.string.StringEncoder;
import io.reactivex.netty.protocol.tcp.client.ConnectionRequest;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.protocol.tcp.server.TcpServerImpl;
import io.reactivex.netty.protocol.text.StringLineDecoder;

public class Proxy {

    public TcpServer<String, String> startTarget() {
        return new TcpServerImpl<ByteBuf, ByteBuf>(0)
                .<String, ByteBuf>addChannelHandlerLast("decoder", StringLineDecoder::new)
                .<String, String>addChannelHandlerLast("encoder", StringEncoder::new)
                .start(connection -> connection.writeAndFlushOnEach(connection.getInput().map(str -> "Proxy echo -> " + str
                                                                                                     + '\n'))
                                               .ignoreElements()
                                               .cast(Void.class));
    }

    public TcpServer<ByteBuf, ByteBuf> startProxy(int targetPort) {
        final ConnectionRequest<ByteBuf, ByteBuf> req =
                        TcpClient.newClient("127.0.0.1", targetPort)
                                 .createConnectionRequest();

        return new TcpServerImpl<ByteBuf, ByteBuf>(0)
                .start(in -> in.writeAndFlushOnEach(req.switchMap(out -> out.writeAndFlushOnEach(in.getInput())
                                                                            .ignoreElements()
                                                                            .cast(ByteBuf.class)
                                                                            .mergeWith(out.getInput()))));
    }

    public static void main(String[] args) {
        Proxy proxy = new Proxy();
        proxy.startProxy(proxy.startTarget().getServerPort()).waitTillShutdown();
    }
}
