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

package io.reactivex.netty.examples.http.ws.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

/**
 * An example of how to process discrete messages on a WebSocket server and send acknowledgments for each message
 * depicting processing complete.
 *
 * All messages follow the format as specified by {@link MessageFrame}.
 */
public final class MessagingServer extends AbstractServerExample {

    public static void main(final String[] args) {

        HttpServer<ByteBuf, ByteBuf> server;

        /*Starts a new HTTP server on an ephemeral port.*/
        server = HttpServer.newServer()
                           /*Starts the server with a request handler.*/
                           .start((req, resp) -> {
                               /*If WebSocket upgrade is requested, then accept the request with an echo handler.*/
                               if (req.isWebSocketUpgradeRequested()) {
                                   return resp.acceptWebSocketUpgrade(wsConn -> {
                                       Observable<WebSocketFrame> in = wsConn.getInput()
                                                                             .filter(AcceptOnlyBinaryFramesFilter.INSTANCE)
                                                                             .cast(BinaryWebSocketFrame.class)
                                                                             .map(f -> {
                                                                                 ByteBuf data = f.content();
                                                                                 /*Convert to ack*/
                                                                                 data.setByte(data.readerIndex(), 1);
                                                                                 return new MessageFrame(data);
                                                                             });
                                       return wsConn.writeAndFlushOnEach(in);
                                   });
                               } else {
                                   /*Else send a NOT FOUND response.*/
                                   return resp.setStatus(HttpResponseStatus.NOT_FOUND);
                               }
                           });

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        setServerPort(server.getServerPort());
    }
}
