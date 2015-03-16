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
import io.netty.buffer.Unpooled;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.serverNew.HttpServer;
import rx.Observable;

public final class HelloWorldServer {

    static final int DEFAULT_PORT = 8090;

    private final int port;

    public HelloWorldServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> startServer() {
        return HttpServer.newServer(port)
                         .enableWireLogging(LogLevel.ERROR)
                         .start((req, resp) -> {
                             System.out.println(req);
                             return req.discardContent()
                                       .concatWith(resp.sendHeaders()
                                                       .write(Observable.just(Unpooled.buffer()
                                                                                      .writeBytes("HelloWorld!".getBytes()))));
                         });
    }

    public static void main(final String[] args) {
        new HelloWorldServer(DEFAULT_PORT).startServer().waitTillShutdown();
    }
}
