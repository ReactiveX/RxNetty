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
package io.reactivex.netty.examples.http.helloworld;

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
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class HelloWorldServer {

    static final int DEFAULT_PORT = 8090;

    private int port;

    public HelloWorldServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                System.out.println("New request received");
                System.out.println(request.getHttpMethod() + " " + request.getUri() + ' ' + request.getHttpVersion());
                for (Map.Entry<String, String> header : request.getHeaders().entries()) {
                    System.out.println(header.getKey() + ": " + header.getValue());
                }

                return request.getContent().materialize()
                        .flatMap(new Func1<Notification<ByteBuf>, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(Notification<ByteBuf> notification) {
                                if (notification.isOnCompleted()) {
                                    return response.writeStringAndFlush("Welcome!!!");
                                } else if (notification.isOnError()) {
                                    return Observable.error(notification.getThrowable());
                                } else {
                                    ByteBuf next = notification.getValue();
                                    System.out.println(next.toString(Charset.defaultCharset()));
                                    return Observable.empty();
                                }
                            }
                        });
            }
        });
        System.out.println("HTTP hello world server started...");
        return server;
    }

    public static void main(final String[] args) {
        new HelloWorldServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
