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

package io.reactivex.netty.examples.http.perf;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.protocol.http.serverNew.HttpServer;

import static rx.Observable.*;

/**
 * A "Hello World" server for doing performance benchmarking.
 *
 * This does optimizations which are not required for normal cases where application processing dominates network and
 * micro object allocation overheads.
 */
public final class PerfHelloWorldServer extends AbstractServerExample {

    public static final String WELCOME_MSG = "Welcome!!";
    private static final byte[] WELCOME_MSG_BYTES = WELCOME_MSG.getBytes();
    private static final String CONTENT_LENGTH_HEADER_VAL = String.valueOf(WELCOME_MSG_BYTES.length); // Does not use int as this omits conversion to string for every response.

    public static void main(final String[] args) {

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer(0)
                           .start((req, resp) ->
                                          resp.setHeader(HttpHeaders.Names.CONTENT_LENGTH, CONTENT_LENGTH_HEADER_VAL)
                                              .flushOnlyOnReadComplete()
                                              .writeBytes(just(WELCOME_MSG_BYTES))/*Write the "Hello World" response*/
                           );

        /*Wait for shutdown if not called from another class (passed an arg)*/
        if (shouldWaitForShutdown(args)) {
            /*When testing the args are set, to avoid blocking till shutdown*/
            server.waitTillShutdown();
        }

        /*Assign the ephemeral port used to a field so that it can be read and used by the caller, if any.*/
        serverPort = server.getServerPort();
    }
}
