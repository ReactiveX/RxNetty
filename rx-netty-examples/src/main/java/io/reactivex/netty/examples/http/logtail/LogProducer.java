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
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Tomasz Bak
 */
public class LogProducer {

    private final int port;
    private final long interval;
    private final String source;

    public LogProducer(int port, int interval) {
        this.port = port;
        this.interval = interval;
        source = "localhost:" + port;
    }

    public HttpServer<ByteBuf, ServerSentEvent> createServer() {
        HttpServer<ByteBuf, ServerSentEvent> server = RxNetty.createHttpServer(port,
                new RequestHandler<ByteBuf, ServerSentEvent>() {
                    @Override
                    public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                   HttpServerResponse<ServerSentEvent> response) {
                        return createReplyHandlerObservable(response);
                    }
                }, PipelineConfigurators.<ByteBuf>sseServerConfigurator());
        System.out.println("Started log producer on port " + port);
        return server;
    }

    private Observable<Void> createReplyHandlerObservable(final HttpServerResponse<ServerSentEvent> response) {
        return Observable.interval(interval, TimeUnit.MILLISECONDS)
                .flatMap(new Func1<Long, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Long interval) {
                        ServerSentEvent data = new ServerSentEvent(
                                Long.toString(interval),
                                "data",
                                LogEvent.randomLogEvent(source).toCSV()
                        );
                        return response.writeAndFlush(data);
                    }
                });
    }

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err.println("ERROR: specify log producer's port number and a message sending interval");
            return;
        }
        int port = Integer.valueOf(args[0]);
        int interval = Integer.valueOf(args[1]);
        new LogProducer(port, interval).createServer().startAndWait();
        System.out.println("LogProducer service terminated");
    }

}
