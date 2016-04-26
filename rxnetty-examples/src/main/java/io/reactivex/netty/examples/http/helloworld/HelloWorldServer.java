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

package io.reactivex.netty.examples.http.helloworld;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.server.HttpServer;

import static rx.Observable.*;

/**
 * An HTTP "Hello World" server.
 *
 * This server sends a response with "Hello World" as the content for any request that it recieves.
 */
public final class HelloWorldServer {

    public static void main(final String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(HelloWorldServer.class);

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer()
                           .start((req, resp) ->
                                          resp.writeString(just("Hello World!"))
                           );

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (env.shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        env.registerServerAddress(server.getServerAddress());
    }
}
