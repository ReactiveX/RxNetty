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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomasz Bak
 */
public class LogAggregator {

    static final int DEFAULT_AG_PORT = 8091;

    private final int port;
    private final int producerPortFrom;
    private final int producerPortTo;
    HttpServer<ByteBuf, ServerSentEvent> server;

    public LogAggregator(int port, int producerPortFrom, int producerPortTo) {
        this.port = port;
        this.producerPortFrom = producerPortFrom;
        this.producerPortTo = producerPortTo;
    }

    public HttpServer<ByteBuf, ServerSentEvent> createAggregationServer() {
        server = RxNetty.createHttpServer(port,
                new RequestHandler<ByteBuf, ServerSentEvent>() {
                    @Override
                    public Observable<Void> handle(final HttpServerRequest<ByteBuf> request,
                                                   final HttpServerResponse<ServerSentEvent> response) {

                        return connectToLogProducers().flatMap(new Func1<ServerSentEvent, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(ServerSentEvent sse) {
                                ServerSentEvent data = new ServerSentEvent(sse.getEventId(), "data", sse.getEventData());
                                return response.writeAndFlush(data);
                            }
                        }).onErrorFlatMap(new Func1<OnErrorThrowable, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(OnErrorThrowable onErrorThrowable) {
                                response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                                return response.close();
                            }
                        });
                    }
                }, PipelineConfigurators.<ByteBuf>sseServerConfigurator());
        System.out.println("Logs aggregator server started...");
        return server;
    }

    private Observable<ServerSentEvent> connectToLogProducers() {
        List<Observable<ServerSentEvent>> oList = new ArrayList<Observable<ServerSentEvent>>(producerPortTo - producerPortFrom + 1);
        for (int i = producerPortFrom; i <= producerPortTo; i++) {
            oList.add(connectToLogProducer(i));
        }
        return Observable.merge(oList);
    }

    private static Observable<ServerSentEvent> connectToLogProducer(int port) {
        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

        return client.submit(HttpClientRequest.createGet("/logstream")).flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                return response.getContent();
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
