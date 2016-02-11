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

package io.reactivex.netty.examples.http.perf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.examples.http.helloworld.HelloWorldServer;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static rx.Observable.*;

/**
 * This is an HTTP server example used to do "Hello World" benchmarks. A "Hello World" benchmark is a good benchmark to
 * analyze library overheads as the application code does not do much.
 *
 * This server is not representative of the otherwise normal {@link HelloWorldServer} which is much simpler. This
 * example instead does micro-optimizations like using {@link HttpServerResponse#flushOnlyOnReadComplete()}, setting
 * the content-length header and storing the response content stream, etc. These optimizations reduce the overheads that
 * are usually not significant for applications that do any "real work".
 */
public final class PerfHelloWorldServer {

    private static final ByteBuf WELCOME_MSG_BUFFER = Unpooled.buffer().writeBytes("Welcome!!".getBytes());

    /*Store the response content Observable to reduce object allocation overheads*/
    private static final Observable<ByteBuf> RESPONSE_CONTENT = just(WELCOME_MSG_BUFFER)
            /*Since, we are using the same buffer for all writes, retain it once before every write, so the buffer does
            * not get recycled. Every write will release the buffer once.*/
            .doOnSubscribe(WELCOME_MSG_BUFFER::retain);

    // Does not use int as this omits conversion to string for every response.
    private static final String CONTENT_LENGTH_HEADER_VAL = String.valueOf(WELCOME_MSG_BUFFER.readableBytes());

    public static void main(final String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(PerfHelloWorldServer.class);

        /*Reduce overhead of event publishing*/
        //RxNetty.disableEventPublishing(); // Uncomment when running load test

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer()
                           .start((req, resp) ->
                                          resp.setHeader(CONTENT_LENGTH, CONTENT_LENGTH_HEADER_VAL)
                                              .flushOnlyOnReadComplete()
                                              .write(RESPONSE_CONTENT)
                           );

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (env.shouldWaitForShutdown(args)) {
            /*When testing the args are set, to avoid blocking till shutdown*/
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
         the caller, if any.*/
        env.registerServerAddress(server.getServerAddress());
    }
}
