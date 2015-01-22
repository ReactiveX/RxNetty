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

package io.reactivex.netty.examples.http.logtail;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * @author Tomasz Bak
 */
public class LogAggregator {

    static final int DEFAULT_AG_PORT = 8091;

    private final int port;
    private final List<Integer> producerPorts;
    HttpServer<ByteBuf, ServerSentEvent> server;

    public LogAggregator(int port, List<Integer> producerPorts) {
        this.port = port;
        this.producerPorts = producerPorts;
    }

    public LogAggregator(int port, int producerPortFrom, int producerPortTo) {
        this.port = port;
        int producerCount = producerPortTo - producerPortFrom + 1;
        producerPorts = new ArrayList<>(producerCount);
        for (int i = 0; i < producerCount; i++) {
            producerPorts.add(producerPortFrom + i);
        }
    }

    public HttpServer<ByteBuf, ServerSentEvent> createAggregationServer() {
        server = RxNetty.newHttpServerBuilder(port,
                new RequestHandler<ByteBuf, ServerSentEvent>() {
                    @Override
                    public Observable<Void> handle(final HttpServerRequest<ByteBuf> request,
                                                   final HttpServerResponse<ServerSentEvent> response) {

                        return connectToLogProducers().flatMap(new Func1<ServerSentEvent, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(ServerSentEvent sse) {
                                return response.writeAndFlush(sse);
                            }
                        });
                    }
                }).enableWireLogging(LogLevel.ERROR).pipelineConfigurator(PipelineConfigurators.<ByteBuf>serveSseConfigurator()).build();
        System.out.println("Logs aggregator server started...");
        return server;
    }

    private Observable<ServerSentEvent> connectToLogProducers() {
        List<Observable<ServerSentEvent>> oList = new ArrayList<Observable<ServerSentEvent>>(producerPorts.size());
        for (int producerPort : producerPorts) {
            oList.add(connectToLogProducer(producerPort));
        }
        return Observable.merge(oList);
    }

    private static Observable<ServerSentEvent> connectToLogProducer(int port) {
        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>clientSseConfigurator());

        return client.submit(HttpClientRequest.createGet("/logstream")).flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                return response.getContent()
                        .doOnNext(new Action1<ServerSentEvent>() {
                            @Override
                            public void call(ServerSentEvent serverSentEvent) {
                                serverSentEvent.retain();
                            }
                        });
            }
        });
    }

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err.println("ERROR: provide log producers port range");
            return;
        }
        int producerPortFrom = Integer.valueOf(args[0]);
        int producerPortTo = Integer.valueOf(args[1]);
        LogAggregator aggregator = new LogAggregator(DEFAULT_AG_PORT, producerPortFrom, producerPortTo);
        aggregator.createAggregationServer().startAndWait();
        System.out.println("Aggregator service terminated");
    }
}
