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

package io.reactivex.netty.examples.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.protocol.http.sse.ServerSentEvent.*;

/**
 * An <a href="http://www.w3.org/TR/eventsource/">Server sent event</a> "Hello World" example server.
 *
 * This server send an infinite Server Sent Event stream for any request that it recieves. The infinite stream publishes
 * an event every 10 milliseconds, with the event data as "Interval => [count starting with 0]"
 */
public final class HelloSseServer extends AbstractServerExample {

    public static void main(final String[] args) {

        HttpServer<ByteBuf, ByteBuf> server;

        /*Starts a new HTTP server on an ephemeral port.*/
        server = HttpServer.newServer()
                           .enableWireLogging(LogLevel.DEBUG)
                           /*Starts the server with a request handler.*/
                           .start((req, resp) ->
                               /*Send an SSE response to all requests.*/
                                          resp.transformToServerSentEvents()
                               /* Write the stream that generates an event every 10 milliseconds*/
                                                  .writeAndFlushOnEach(Observable.interval(10, TimeUnit.MILLISECONDS)
                                           /*If the channel puts backpressure, then drop data.*/
                                                                               .onBackpressureDrop()
                                           /*Convert the tick generated to a ServerSentEvent object.*/
                                                                               .map(aLong -> withData(
                                                                                       "Interval => " + aLong)))
                           );

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        setServerPort(server.getServerPort());
    }
}
