/*
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.examples.http.file;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.RequestHandlerWithErrorMapper;
import io.reactivex.netty.protocol.http.server.file.FileErrorResponseMapper;
import io.reactivex.netty.protocol.http.server.file.WebappFileRequestHandler;

public class HttpFileServer {
    static final int DEFAULT_PORT = 8103;

    private final int port;

    public HttpFileServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        return RxNetty.createHttpServer(port,
                RequestHandlerWithErrorMapper.from(
                        new WebappFileRequestHandler(),
                        new FileErrorResponseMapper()));
    }

    public static void main(String[] args) {
        System.out.println("HTTP file server starting on port " + DEFAULT_PORT + " ...");
        new HttpFileServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
