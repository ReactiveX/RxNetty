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

package io.reactivex.netty.examples.tcp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.client.ConnectionRequest;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import rx.Observable;

import java.net.SocketAddress;

import static java.nio.charset.Charset.*;

/**
 * An example to demonstrate how to write a simple TCP proxy.
 *
 * The intent here is <em>NOT</em> to prescribe how to write a fully functional proxy, which would otherwise require
 * appropriate routing on the origin endpoints, etc. Instead, it is to demonstrate how to write a server that forwards
 * the received data on a connection, as is, to another server using an RxNetty client.
 *
 * This example starts an embedded target server, which is a simple TCP server that echoes the messages recieved on any
 * connection recieves. The proxy server then forwards all received data on any connection to this target server. Any
 * messages received from the target server are sent back to the caller, after prepending "proxy =>" to every message,
 * to demonstrate that the data is proxied.
 */
public final class ProxyServer {

    public static void main(final String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(ProxyServer.class);

        /*Start an embedded target server and a TCP client pointing to that.*/
        final TcpClient<ByteBuf, ByteBuf> targetClient = TcpClient.newClient(startTargetServer());
        /*Create a new connection request, each subscription to which creates a new connection.*/
        ConnectionRequest<ByteBuf, ByteBuf> connReq = targetClient.createConnectionRequest();

        TcpServer<ByteBuf, ByteBuf> server;

        /*Starts a new HTTP server on an ephemeral port which acts as a proxy to the target server started above.*/
        server = TcpServer.newServer()
                          .enableWireLogging("proxy-server", LogLevel.DEBUG)
                          .start(serverConn ->
                                         serverConn.writeStringAndFlushOnEach(
                                                 connReq.flatMap(clientConn -> {
                                                                     Observable<String> clientOutput =
                                                                             clientConn.getInput()
                                                                                       .map(bb -> bb.toString(
                                                                                               defaultCharset()))
                                                                                       .map(msg -> "proxy => " + msg);
                                                                     return clientConn
                                                                             .writeAndFlushOnEach(serverConn.getInput())
                                                                             .cast(String.class)
                                                                             .mergeWith(clientOutput);
                                                                 }
                                                 )
                                         )
                          );

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (env.shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        env.registerServerAddress(server.getServerAddress());
    }

    private static SocketAddress startTargetServer() {
        /*A new server that echoes what it receives on any connection*/
        return TcpServer.newServer()
                /*Starts the server with the echo connection handler.*/
                        .start(c -> c.writeStringAndFlushOnEach(c.getInput()
                                                                 .map(bb -> bb.toString(defaultCharset()))
                                                                 .map(msg -> "echo => " + msg)
                               )
                        )
                        .getServerAddress();
    }
}
