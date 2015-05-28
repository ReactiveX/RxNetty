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

package io.reactivex.netty.examples.http.ws.echo;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.protocol.http.server.HttpServer;

import static io.reactivex.netty.protocol.http.ws.server.WebSocketHandlers.*;
import static java.nio.charset.Charset.*;

/**
 * An Web socket echo server that echoes back all frames received.
 */
public final class WebSocketEchoServer extends AbstractServerExample {

    public static void main(final String[] args) {

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer(0)
                           .start(acceptAllUpgrades(conn ->
                                                            conn.writeAndFlushOnEach(conn.getInputForWrite()
                                                                                         .doOnNext(frame -> {
                                                                                             String msg =
                                                                                                     frame.content()
                                                                                                          .toString(
                                                                                                                  defaultCharset());
                                                                                             logger.info(msg);
                                                                                         }))
                                  )
                           );

        /*Wait for shutdown if not called from another class (passed an arg)*/
        if (shouldWaitForShutdown(args)) {
            /*When testing the args are set, to avoid blocking till shutdown*/
            server.awaitShutdown();
        }

        /*Assign the ephemeral port used to a field so that it can be read and used by the caller, if any.*/
        setServerPort(server.getServerPort());
    }
}
