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

package io.reactivex.netty.examples.tcp.interceptors.simple;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ContentSource;
import io.reactivex.netty.channel.SimpleAbstractDelegatingConnection;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.util.StringLineDecoder;
import rx.Observable;

import java.net.SocketAddress;

/**
 * A client for {@link InterceptingServer}, which follows a simple text based, new line delimited message protocol.
 * The client expects a "Hello" as the first message and then the echo of what it sends to the server. This client,
 * sends a "Hello World" to the server and expects an echo.
 *
 * There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server ({@link InterceptingServer})
 * on an ephemeral port, send a "Hello World!" message to the server and print the response.
 *
 * <h2>After starting {@link InterceptingServer}</h2>
 *
 * If you want to see how {@link InterceptingServer} work, you can run {@link InterceptingServer} by yourself and then
 * pass the port on which the server started to this class as a program argument:
 *
 <PRE>
 java io.reactivex.netty.examples.tcp.interceptors.simple.InterceptingClient [server port]
 </PRE>
 *
 * <h2>Existing TCP server</h2>
 *
 * You can also use this client to connect to an already running TCP server (different than
 * {@link InterceptingServer}) by passing the port and host of the existing server similar to the case above:
 *
 <PRE>
 java io.reactivex.netty.examples.tcp.interceptors.simple.InterceptingClient [server port] [server host]
 </PRE>
 * If the server host is omitted from the above, it defaults to "127.0.0.1"
 *
 * In all the above usages, this client will print the response received from the server.
 *
 * @see InterceptingServer Default server for this client.
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
        SocketAddress serverAddress = getServerAddress(InterceptingServer.class, args);

        TcpClient.newClient(serverAddress)
                 .<ByteBuf, String>addChannelHandlerLast("string-line-decoder", StringLineDecoder::new)
                 .intercept()
                 .next(provider -> () -> provider.newConnectionRequest()
                                                 .map(HelloTruncatingConnection::new))
                 .finish()
                 .createConnectionRequest()
                 .flatMap(connection -> connection.writeString(Observable.just("Hello World!"))
                                                  .cast(String.class)
                                                  .mergeWith(connection.getInput())
                 )
                 .take(1)
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
}
