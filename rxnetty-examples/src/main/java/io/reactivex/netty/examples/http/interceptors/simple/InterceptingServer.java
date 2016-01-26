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

package io.reactivex.netty.examples.http.interceptors.simple;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.examples.http.interceptors.transformation.TransformingInterceptorsServer;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerInterceptorChain;
import io.reactivex.netty.protocol.http.server.HttpServerInterceptorChain.Interceptor;

import static rx.Observable.*;

/**
 * An HTTP "Hello World" server which also demonstrates adding simple interceptors that do not change request/response
 * object types. For more complex example of transformations, see {@link TransformingInterceptorsServer}
 *
 * This server sends a response with "Hello World" as the content for any request that it receives.
 */
public class InterceptingServer extends AbstractServerExample {

    public static final String INTERCEPTOR_HEADER_NAME = "X-from-interceptor";

    public static void main(final String[] args) {

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer()
                           .enableWireLogging(LogLevel.DEBUG)
                           .start(HttpServerInterceptorChain.startRaw()
                                                            .next(addHeader())
                                                            .end((req, resp) -> resp.writeString(just("Hello World!")))
                           );

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        setServerPort(server.getServerPort());
    }

    private static Interceptor<ByteBuf, ByteBuf> addHeader() {
        return handler -> (request, response) -> {
            String clientHeader = request.containsHeader(INTERCEPTOR_HEADER_NAME)
                                            ? request.getHeader(INTERCEPTOR_HEADER_NAME)
                                            : "none";

            return handler.handle(request.addHeader(INTERCEPTOR_HEADER_NAME, "server"),
                                  response.addHeader(INTERCEPTOR_HEADER_NAME, clientHeader)
                                          .addHeader(INTERCEPTOR_HEADER_NAME, "server"));
        };
    }
}
