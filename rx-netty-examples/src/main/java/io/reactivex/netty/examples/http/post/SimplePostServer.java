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

package io.reactivex.netty.examples.http.post;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.nio.charset.Charset;

/**
 * @author Tomasz Bak
 */
public class SimplePostServer {
    static final int DEFAULT_PORT = 8102;

    private int port;

    public SimplePostServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                return request.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                }).reduce("", new Func2<String, String, String>() {
                    @Override
                    public String call(String accumulator, String value) {
                        return accumulator + value;
                    }
                }).flatMap(new Func1<String, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(String clientMessage) {
                        return response.writeStringAndFlush(clientMessage.toUpperCase());
                    }
                });
            }
        });
        System.out.println("Simple POST server started...");
        return server;
    }

    public static void main(final String[] args) {
        new SimplePostServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
