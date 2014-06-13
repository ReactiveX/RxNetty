/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.examples.http.wordcounter;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Tomasz Bak
 */
public final class WordCounterServer {

    static final int DEFAULT_PORT = 8097;

    private int port;

    public WordCounterServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            public AtomicInteger counter = new AtomicInteger();

            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                return request.getContent().materialize()
                        .flatMap(new Func1<Notification<ByteBuf>, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(Notification<ByteBuf> notification) {
                                if (notification.isOnCompleted()) {
                                    return response.writeStringAndFlush(counter.toString());
                                } else if (notification.isOnError()) {
                                    return Observable.error(notification.getThrowable());
                                } else {
                                    countWords(notification.getValue());
                                    return Observable.empty();
                                }
                            }

                            private void countWords(ByteBuf byteBuf) {
                                String text = byteBuf.toString(Charset.defaultCharset());
                                counter.addAndGet(text.split("\\s{1,}").length);
                            }
                        });
            }
        });
        System.out.println("Started word counter server...");
        return server;
    }

    public static void main(final String[] args) {
        new WordCounterServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
