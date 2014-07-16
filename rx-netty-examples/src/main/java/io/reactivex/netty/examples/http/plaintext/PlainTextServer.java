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

package io.reactivex.netty.examples.http.plaintext;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

/**
 * @author Nitesh Kant
 */
public final class PlainTextServer {

    static final int DEFAULT_PORT = 8111;
    public static final String WELCOME_MSG = "Welcome!!";
    private static final byte[] WELCOME_MSG_BYTES = WELCOME_MSG.getBytes();
    private static final String CONTENT_LENGTH_HEADER_VAL = String.valueOf(WELCOME_MSG_BYTES.length); // Does not use int as this omits conversion to string for every response.

    private final int port;

    public PlainTextServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server =
                RxNetty.createHttpServer(port,
                                         new RequestHandler<ByteBuf, ByteBuf>() {
                                             @Override
                                             public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                                            final HttpServerResponse<ByteBuf> response) {
                                                 response.getHeaders().set(HttpHeaders.Names.CONTENT_LENGTH,
                                                                           CONTENT_LENGTH_HEADER_VAL); // This makes RxNetty write a single Full response as opposed to writing header, content & lastHttpContent.
                                                 ByteBuf content = response.getAllocator()
                                                                           .buffer(WELCOME_MSG_BYTES.length)
                                                                           .writeBytes(WELCOME_MSG_BYTES);
                                                 response.write(content);
                                                 return response.close(
                                                         false); // Let RxNetty take care of flushing appropriately. Do NOT use when processing in a different thread.
                                             }
                                         });

        return server;
    }

    public static void main(final String[] args) throws InterruptedException {
        HttpServer<ByteBuf, ByteBuf> server = new PlainTextServer(DEFAULT_PORT).createServer();
        server.start();
        System.out.println("HTTP plain text server started at port: " + server.getServerPort());
        server.waitTillShutdown();
    }
}
