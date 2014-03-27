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
package io.reactivex.netty.examples.java;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class HttpWelcomeServer {

    public static void main(final String[] args) {
        final int port = 8080;

        RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                System.out.println("New request recieved");
                System.out.println(request.getHttpMethod() + " " + request.getUri() + ' ' + request.getHttpVersion());
                for (Map.Entry<String, String> header : request.getHeaders().entries()) {
                    System.out.println(header.getKey() + ": " + header.getValue());
                }
                // This does not consume request content, need to figure out an elegant/correct way of doing that.
                return response.writeStringAndFlush("Welcome!!! \n\n");
            }
        }).startAndWait();
    }
}
