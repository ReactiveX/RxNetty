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

package io.reactivex.netty.examples.http.streaming;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.protocol.http.serverNew.HttpServer;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public final class StreamingServer {

    static final int DEFAULT_PORT = 8010;

    private final int port;

    public StreamingServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> startServer() {
        return HttpServer.newServer(port)
                         .start((req, resp) -> {
                             System.out.println(req);
                             final AtomicInteger flushBuckets = new AtomicInteger();
                             return req.getContent()
                                       .<Void>map(bb -> {
                                           System.out.println(bb.toString(Charset.defaultCharset()));
                                           return null;
                                       })
                                       .ignoreElements()
                                       .concatWith(resp.sendHeaders()
                                                       .write(Observable.range(1, 10000000)
                                                                        .map(anInt -> {
                                                                            if (anInt % 1000 == 0) {
                                                                                System.out.println(
                                                                                        "Writing item # " + anInt);
                                                                            }
                                                                            return Unpooled.buffer()
                                                                                           .writeBytes(
                                                                                                   ("Interval: " +
                                                                                                    anInt + '\n')
                                                                                                           .getBytes());
                                                                        }),
                                                              bb -> flushBuckets.incrementAndGet() % 100 == 0));
                         });
    }

    public static void main(final String[] args) {
        new StreamingServer(DEFAULT_PORT).startServer().waitTillShutdown();
    }
}
