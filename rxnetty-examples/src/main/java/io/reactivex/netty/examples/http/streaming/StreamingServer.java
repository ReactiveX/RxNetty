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

package io.reactivex.netty.examples.http.streaming;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * An HTTP server that sends an infinite HTTP chunked response emitting a number every second.
 */
public final class StreamingServer extends AbstractServerExample {

    public static void main(final String[] args) {

        HttpServer<ByteBuf, ByteBuf> server;

        /*Starts a new HTTP server on an ephemeral port.*/
        server = HttpServer.newServer()
                           .enableWireLogging(LogLevel.DEBUG)
                            /*Starts the server with a request handler.*/
                           .start((req, resp) ->
                                          /*Write a message every 10 milliseconds*/
                                          resp.writeStringAndFlushOnEach(
                                                  Observable.interval(10, TimeUnit.MILLISECONDS)
                                                          /*If the channel is backed up with data, drop the numbers*/
                                                          .onBackpressureDrop()
                                                          /*Convert the number to a string.*/
                                                          .map(aLong -> "Interval =>" + aLong)
                                          )
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
