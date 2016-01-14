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
import io.reactivex.netty.channel.AbstractDelegatingConnection;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ContentSource;
import io.reactivex.netty.channel.SimpleAbstractDelegatingConnection;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.util.StringLineDecoder;
import rx.Observable;

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
public final class InterceptingClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress serverAddress = getServerAddress(TransformingInterceptorsServer.class, args);

        TcpClient.newClient(serverAddress)
                .<ByteBuf, String>addChannelHandlerLast("string-line-decoder", StringLineDecoder::new)
                .intercept()
                .next(provider -> () -> provider.newConnectionRequest()
                                                .map(HelloTruncatingConnection::new))
                .nextWithReadTransform(provider -> () -> provider.newConnectionRequest()
                                                                 .map(ReadIntegerConnection::new))
                .nextWithWriteTransform(provider -> () -> provider.newConnectionRequest()
                                                                  .map(WriteIntegerConnection::new))
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

    private static class HelloTruncatingConnection extends SimpleAbstractDelegatingConnection<String, ByteBuf> {

        public HelloTruncatingConnection(Connection<String, ByteBuf> c) {
            super(c);
        }

        @Override
        public ContentSource<String> getInput() {
            return getDelegate().getInput()
                                .transform(source -> source.skip(1));
        }
    }

    private static class ReadIntegerConnection extends AbstractDelegatingConnection<String, ByteBuf, Integer, ByteBuf> {

        public ReadIntegerConnection(Connection<String, ByteBuf> c) {
            super(c);
        }

        @Override
        public ContentSource<Integer> getInput() {
            return getDelegate().getInput()
                                .transform(source -> source.filter(s1 -> !s1.isEmpty())
                                                           .flatMap(s -> Observable.from(s.split(" "))
                                                                                   .map(Integer::parseInt)));
        }
    }

    private static class WriteIntegerConnection
            extends AbstractDelegatingConnection<Integer, ByteBuf, Integer, Integer> {

        public WriteIntegerConnection(Connection<Integer, ByteBuf> c) {
            super(c, new Transformer<Integer, ByteBuf>() {
                @Override
                public List<ByteBuf> transform(Integer toTransform, ByteBufAllocator allocator) {
                    ByteBuf b = allocator.buffer().writeInt(toTransform).writeChar('\n');
                    return Collections.singletonList(b);
                }
            });
        }

        @Override
        public ContentSource<Integer> getInput() {
            return getDelegate().getInput();
        }
    }
}
