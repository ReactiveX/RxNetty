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

package io.reactivex.netty.examples.tcp.streaming;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.codec.StringLineDecoder;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.tcp.client.TcpClient;

import java.net.SocketAddress;

/**
 * A TCP streaming example.
 *
 * This example assumes that the server to which this client connects (using the below mentioned option), sends an
 * infinite stream of new line seperated strings. This client only takes 10 items from that infinite stream and then
 * terminates the connection.
 *
 * There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server ({@link StreamingServer}) on
 * an ephemeral port and then send an HTTP request to that server and print the response.
 *
 * <h2>After starting {@link StreamingServer}</h2>
 *
 * If you want to see how {@link StreamingServer} work, you can run {@link StreamingServer} by yourself and then pass
 * the port on which the server started to this class as a program argument:
 *
 <PRE>
 java io.reactivex.netty.examples.tcp.streaming.StreamingClient [server port]
 </PRE>
 *
 * <h2>Existing TCP server</h2>
 *
 * You can also use this client to connect to an existing TCP server (different than {@link StreamingServer}) by
 * passing the port and host of the existing server similar to the case above:
 *
 <PRE>
 java io.reactivex.netty.examples.tcp.streaming.StreamingClient [server port] [server host]
 </PRE>
 * If the server host is omitted from the above, it defaults to "127.0.0.1"
 *
 * In all the above usages, this client will print the response received from the server.
 *
 * @see StreamingServer Default server for this client.
 */
public final class StreamingClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress serverAddress = getServerAddress(StreamingServer.class, args);

        /*Create a new client for the server address*/
        TcpClient.<ByteBuf, ByteBuf>newClient(serverAddress)
                /*Create a new connection request, each subscription creates a new connection*/
                 .createConnectionRequest()
                /* Add a decoder that reads the input and splits it on new line, this makes the output predictable, as
                 * opposed to reading raw bytes*/
                 .<ByteBuf, String>addChannelHandlerLast("string-decoder", StringLineDecoder::new)
                /*on successful connection, start reading the input*/
                 .flatMap(Connection::getInput)
                /*Take 10 strings*/
                 .take(10)
                /*Block till the response comes to avoid JVM exit.*/
                 .toBlocking()
                /*Print each content chunk*/
                 .forEach(logger::info);
    }
}
