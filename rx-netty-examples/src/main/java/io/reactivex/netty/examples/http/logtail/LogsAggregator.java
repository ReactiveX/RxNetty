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
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomasz Bak
 */
public class LogsAggregator {

    private final int port;
    private int producerPortFrom;
    private int producerPortTo;
    HttpServer<ByteBuf, ServerSentEvent> server;

    public LogsAggregator(int port, int producerPortFrom, int producerPortTo) {
        this.port = port;
        this.producerPortFrom = producerPortFrom;
        this.producerPortTo = producerPortTo;
    }

    private Observable<ServerSentEvent> connectToLogProducer(int port) {
        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

        return client.submit(HttpClientRequest.createGet("/logstream")).flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                return response.getContent();
            }
        });
    }

    private Observable<ServerSentEvent> connectToLogProducers() {
        List<Observable<ServerSentEvent>> oList = new ArrayList<Observable<ServerSentEvent>>(producerPortTo - producerPortFrom + 1);
        for (int i = producerPortFrom; i <= producerPortTo; i++) {
            oList.add(connectToLogProducer(i));
        }
        return Observable.merge(oList);
    }

    private void startAggregationServer() {
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
                        }).materialize().flatMap(new Func1<Notification<Void>, Observable<? extends Void>>() {
                            @Override
                            public Observable<? extends Void> call(Notification<Void> notification) {
                                if (notification.isOnError()) {
                                    System.err.println("Connection to one of the clients failed");
                                    return Observable.<Void>error(notification.getThrowable());
                                }
                                return Observable.empty();
                            }
                        });

                    }
                }, PipelineConfigurators.<ByteBuf>sseServerConfigurator());
        server.startAndWait();
    }

    public static void main(final String[] args) {
        int port = 8080;
        int producerPortFrom = 8081;
        int producerPortTo = 8082;
        if (args.length > 2) {
            port = Integer.valueOf(args[0]);
            producerPortFrom = Integer.valueOf(args[1]);
            producerPortTo = Integer.valueOf(args[2]);
        }
        LogsAggregator aggregator = new LogsAggregator(port, producerPortFrom, producerPortTo);
        aggregator.startAggregationServer();
        System.out.println("Aggregator service terminated");
    }
}
