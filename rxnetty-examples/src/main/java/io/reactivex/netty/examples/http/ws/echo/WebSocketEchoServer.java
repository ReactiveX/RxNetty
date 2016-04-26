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

package io.reactivex.netty.examples.http.ws.echo;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

/**
 * An example to demonstrate how to write a WebSocket client. This example accepts WebSocket upgrade as well as normal
 * HTTP requests.
 *
 * If a request to WebSockets is requested, then this server echoes all received frames, else if the request is a
 * "/hello" request it sends a "Hello World!" response.
 */
public final class WebSocketEchoServer {

    public static void main(final String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(WebSocketEchoServer.class);

        HttpServer<ByteBuf, ByteBuf> server;

        /*Starts a new HTTP server on an ephemeral port.*/
        server = HttpServer.newServer()
                           .enableWireLogging(LogLevel.DEBUG)
                           .start((req, resp) -> {
                               /*If WebSocket upgrade is requested, then accept the request with an echo handler.*/
                               if (req.isWebSocketUpgradeRequested()) {
                                   return resp.acceptWebSocketUpgrade(wsConn -> wsConn.writeAndFlushOnEach(wsConn.getInput()));
                               } else if (req.getUri().startsWith("/hello")) {
                                   /*If upgrade is not requested and the URI is "hello" then send an "Hello World"
                                   response*/
                                   return resp.writeString(Observable.just("Hello World"));
                               } else {
                                   /*Else send a NOT FOUND response.*/
                                   return resp.setStatus(HttpResponseStatus.NOT_FOUND);
                               }
                           });

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (env.shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        env.registerServerAddress(server.getServerAddress());
    }
}
