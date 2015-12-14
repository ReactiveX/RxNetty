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

package io.reactivex.netty.examples.tcp.interceptors.transformation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.reactivex.netty.channel.AbstractDelegatingConnection;
import io.reactivex.netty.channel.AbstractDelegatingConnection.Transformer;
import io.reactivex.netty.channel.ContentSource;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.examples.tcp.interceptors.simple.InterceptingServer;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.protocol.tcp.server.TcpServerInterceptorChain;
import io.reactivex.netty.protocol.tcp.server.TcpServerInterceptorChain.Interceptor;
import io.reactivex.netty.protocol.tcp.server.TcpServerInterceptorChain.TransformingInterceptor;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A TCP echo server that echoes all input it receives on any connection, after prepending the input with a fixed
 * string. <p>
 *
 * This example demonstrates the usage of server side interceptors which do data transformations.
 * For interceptors requiring no data transformation see {@link InterceptingServer}
 *
 * This example just aims to demonstrate how to write the simplest TCP server, it is however, not of much use in general
 * primarily because it reads unstructured data i.e. there are no boundaries that define what constitutes "a message".
 * In order to define such boundaries, one would typically add a {@link ChannelHandler} that converts the read raw
 * {@code ByteBuffer} to a structured message.
 */
public final class TransformingInterceptorsServer extends AbstractServerExample {

    public static void main(final String[] args) {

        TcpServer<ByteBuf, ByteBuf> server;

        /*Starts a new TCP server on an ephemeral port.*/
        server = TcpServer.newServer(0)
                          /*Starts the server with a connection handler.*/
                .start(TcpServerInterceptorChain.startRaw(sendHello())
                                                .nextWithReadTransform(readStrings())
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
     * Logs every new connection.
     *
     * @return Interceptor for logging new connections.
     */
    private static Interceptor<ByteBuf, ByteBuf> sendHello() {
        return in -> newConnection -> newConnection.writeString(Observable.just("Hello"))
                                                   .concatWith(in.handle(newConnection));
    }

    /**
     * Converts read bytes to string.
     *
     * @return Interceptor for logging new connections.
     */
    private static TransformingInterceptor<ByteBuf, ByteBuf, String, ByteBuf> readStrings() {
        return in -> newConnection ->
                in.handle(new AbstractDelegatingConnection<ByteBuf, ByteBuf, String, ByteBuf>(newConnection) {
                    @Override
                    public ContentSource<String> getInput() {
                        return newConnection.getInput()
                                            .transform(o -> o.map(bb -> bb.toString(Charset.defaultCharset())));
                    }
                });
    }

    private static TransformingInterceptor<String, ByteBuf, String, String> writeStrings() {
        return in -> newConnection ->
                in.handle(new AbstractDelegatingConnection<String, ByteBuf, String, String>(newConnection,
                                                                                            transformStringToBytes()) {
                    @Override
                    public ContentSource<String> getInput() {
                        return newConnection.getInput();
                    }
                });
    }

    private static TransformingInterceptor<String, String, Integer, Integer> readAndWriteInts() {
        return in -> newConnection ->
                in.handle(new AbstractDelegatingConnection<String, String, Integer, Integer>(newConnection,
                                                                                            transformIntegerToString()) {
                    @Override
                    public ContentSource<Integer> getInput() {
                        return newConnection.getInput()
                                            .transform(o -> o.map(String::trim).map(Integer::parseInt));
                    }
                });
    }

    /**
     * New {@link ConnectionHandler} that echoes all data received.
     *
     * @return Connection handler.
     */
    private static ConnectionHandler<Integer, Integer> numberIncrementingHandler() {
        return conn -> conn.writeAndFlushOnEach(conn.getInput().map(anInt -> ++anInt));
    }

    private static Transformer<String, ByteBuf> transformStringToBytes() {

        return new Transformer<String, ByteBuf>() {
            @Override
            public List<ByteBuf> transform(String toTransform, ByteBufAllocator allocator) {
                return Collections.singletonList(allocator.buffer().writeBytes(toTransform.getBytes()));
            }
        };
    }

    private static Transformer<Integer, String> transformIntegerToString() {
        return new Transformer<Integer, String>() {
            @Override
            public List<String> transform(Integer toTransform, ByteBufAllocator allocator) {
                return Arrays.asList(String.valueOf(toTransform), String.valueOf(++toTransform));
            }
        };
    }
}
