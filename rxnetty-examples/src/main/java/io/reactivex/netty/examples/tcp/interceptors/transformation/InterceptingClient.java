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
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.util.StringLineDecoder;
import org.slf4j.Logger;
import rx.Observable;
import rx.functions.Func1;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

/**
 * A client for {@link TransformingInterceptorsServer}, which follows a simple text based, new line delimited
 * message protocol.
 * The client expects a "Hello" as the first message and then next two integers for every integer sent by the client.
 *
 * There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server
 * ({@link TransformingInterceptorsServer})
 * on an ephemeral port, send a "Hello World!" message to the server and print the response.
 *
 * <h2>After starting {@link TransformingInterceptorsServer}</h2>
 *
 * If you want to see how {@link TransformingInterceptorsServer} work, you can run
 * {@link TransformingInterceptorsServer} by yourself and then
 * pass the port on which the server started to this class as a program argument:
 *
 <PRE>
 java io.reactivex.netty.examples.tcp.interceptors.transformation.InterceptingClient [server port]
 </PRE>
 *
 * <h2>Existing TCP server</h2>
 *
 * You can also use this client to connect to an already running TCP server (different than
 * {@link TransformingInterceptorsServer}) by passing the port and host of the existing server similar to the case above:
 *
 <PRE>
 java io.reactivex.netty.examples.tcp.interceptors.transformation.InterceptingClient [server port] [server host]
 </PRE>
 * If the server host is omitted from the above, it defaults to "127.0.0.1"
 *
 * In all the above usages, this client will print the response received from the server.
 *
 * @see TransformingInterceptorsServer Default server for this client.
 */
public final class InterceptingClient {

    public static void main(String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(InterceptingClient.class);
        Logger logger = env.getLogger();

        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress serverAddress = env.getServerAddress(TransformingInterceptorsServer.class, args);

        TcpClient.newClient(serverAddress)
                .<ByteBuf, String>addChannelHandlerLast("string-line-decoder", StringLineDecoder::new)
                .intercept()
                .next(provider -> () -> provider.newConnectionRequest().map(skipHello()))
                .nextWithReadTransform(provider -> () -> provider.newConnectionRequest().map(readIntegers()))
                .nextWithWriteTransform(provider -> () -> provider.newConnectionRequest().map(writeIntegers()))
                .finish()
                .createConnectionRequest()
                .flatMap(connection -> connection.write(Observable.just(1))
                                                 .cast(Integer.class)
                                                 .mergeWith(connection.getInput())
                )
                .take(2)
                .map(Object::toString)
                .toBlocking()
                .forEach(logger::info);
    }

    private static Func1<Connection<String, ByteBuf>, Connection<String, ByteBuf>> skipHello() {
        return c -> c.transformRead(o -> o.skip(1));
    }

    private static Func1<Connection<String, ByteBuf>, Connection<Integer, ByteBuf>> readIntegers() {
        return c -> c.transformRead(o -> o.filter(s1 -> !s1.isEmpty())
                                          .flatMap(s -> Observable.from(s.split(" ")).map(Integer::parseInt)));
    }

    private static Func1<? super Connection<Integer, ByteBuf>, Connection<Integer, Integer>> writeIntegers() {
        return c -> c.transformWrite(new AllocatingTransformer<Integer, ByteBuf>() {
            @Override
            public List<ByteBuf> transform(Integer toTransform, ByteBufAllocator allocator) {
                ByteBuf b = allocator.buffer().writeBytes((toTransform.toString() + "\n").getBytes());
                return Collections.singletonList(b);
            }
        });
    }
}
