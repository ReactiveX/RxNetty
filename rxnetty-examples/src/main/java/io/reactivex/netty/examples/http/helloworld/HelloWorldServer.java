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

package io.reactivex.netty.examples.http.helloworld;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.protocol.http.serverNew.HttpServer;

import static rx.Observable.*;

/**
 * An HTTP "Hello World" server. It returns an "Hello World" response for all requests received.
 */
public final class HelloWorldServer extends AbstractServerExample {

    public static void main(final String[] args) {

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer(8888)
                           .start((req, resp) ->
                                          req.discardContent() /*Discard content since we do not read it.*/
                                             .concatWith(resp.sendHeaders()
                                                              /*Write the "Hello World" response*/
                                                             .writeString(just("HelloWorld!")))
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
