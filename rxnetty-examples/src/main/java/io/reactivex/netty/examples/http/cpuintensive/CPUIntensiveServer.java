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

package io.reactivex.netty.examples.http.cpuintensive;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class CPUIntensiveServer {

    static final int DEFAULT_PORT = 8790;
    public static final String IN_EVENT_LOOP_HEADER_NAME = "in_event_loop";

    private final int port;

    public CPUIntensiveServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server =
                RxNetty.newHttpServerBuilder(port, new RequestHandler<ByteBuf, ByteBuf>() {
                    @Override
                    public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                   final HttpServerResponse<ByteBuf> response) {
                        printRequestHeader(request);
                        response.getHeaders().set(IN_EVENT_LOOP_HEADER_NAME,
                                                  response.getChannel().eventLoop()
                                                          .inEventLoop());
                        response.writeString("Welcome!!");
                        return response.close(false);
                    }
                }).pipelineConfigurator(PipelineConfigurators.<ByteBuf, ByteBuf>httpServerConfigurator())
                       .withRequestProcessingThreads(50) /*Uses a thread pool of 50 threads to process the requests.*/
                       .build();
        return server;
    }

    public void printRequestHeader(HttpServerRequest<ByteBuf> request) {
        System.out.println("New request received");
        System.out.println(request.getHttpMethod() + " " + request.getUri() + ' ' + request.getHttpVersion());
        for (Map.Entry<String, String> header : request.getHeaders().entries()) {
            System.out.println(header.getKey() + ": " + header.getValue());
        }
    }

    public static void main(final String[] args) {
        new CPUIntensiveServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
