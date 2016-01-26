/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.examples.tcp.interceptors.transformation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.reactivex.netty.channel.AllocatingTransformer;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.examples.tcp.interceptors.simple.InterceptingServer;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.protocol.tcp.server.TcpServerInterceptorChain;
import io.reactivex.netty.protocol.tcp.server.TcpServerInterceptorChain.Interceptor;
import io.reactivex.netty.protocol.tcp.server.TcpServerInterceptorChain.TransformingInterceptor;
import io.reactivex.netty.util.StringLineDecoder;
import rx.Observable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A TCP server that follows a simple text based, new line delimited message protocol.
 * The server sends a "Hello" as the first message and then expects a stream of integers to be sent by the client.
 * For every integer received, it sends the next two integers in a single message separated by a space.<p>
 *
 * This example demonstrates the usage of server side interceptors which do data transformations.
 * For interceptors requiring no data transformation see {@link InterceptingServer}
 */
public final class TransformingInterceptorsServer extends AbstractServerExample {

    public static void main(final String[] args) {

        TcpServer<String, ByteBuf> server;

        /*Starts a new TCP server on an ephemeral port.*/
        server = TcpServer.newServer(0)
                          .<String, ByteBuf>addChannelHandlerLast("string-line-decoder", StringLineDecoder::new)
                          .start(TcpServerInterceptorChain.start(sendHello())
                                                          .nextWithWriteTransform(writeStrings())
                                                          .nextWithTransform(readAndWriteInts())
                                                          .end(numberIncrementingHandler()));

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        setServerPort(server.getServerPort());
    }

    /**
     * Sends a hello on accepting a new connection.
     */
    private static Interceptor<String, ByteBuf> sendHello() {
        return in -> newConnection -> newConnection.writeString(Observable.just("Hello\n"))
                                                   .concatWith(in.handle(newConnection));
    }

    private static TransformingInterceptor<String, ByteBuf, String, String> writeStrings() {
        return in -> newConnection ->
                in.handle(newConnection.transformWrite(transformStringToBytes()));
    }

    private static TransformingInterceptor<String, String, Integer, Integer> readAndWriteInts() {
        return in -> newConnection -> in.handle(newConnection.transformRead(o -> o.map(String::trim)
                                                                                  .filter(s -> !s.isEmpty())
                                                                                  .map(Integer::parseInt))
                                                             .transformWrite(transformStringToInteger())
        );
    }

    /**
     * New {@link ConnectionHandler} that echoes all data received.
     *
     * @return Connection handler.
     */
    private static ConnectionHandler<Integer, Integer> numberIncrementingHandler() {
        return conn -> conn.writeAndFlushOnEach(conn.getInput().map(anInt -> ++anInt));
    }

    private static AllocatingTransformer<String, ByteBuf> transformStringToBytes() {

        return new AllocatingTransformer<String, ByteBuf>() {
            @Override
            public List<ByteBuf> transform(String toTransform, ByteBufAllocator allocator) {
                return Collections.singletonList(allocator.buffer().writeBytes(toTransform.getBytes()));
            }
        };
    }

    private static AllocatingTransformer<Integer, String> transformStringToInteger() {
        return new AllocatingTransformer<Integer, String>() {
            @Override
            public List<String> transform(Integer toTransform, ByteBufAllocator allocator) {
                return Arrays.asList(String.valueOf(toTransform), " ", String.valueOf(++toTransform), "\n");
            }
        };
    }
}
