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

package io.reactivex.netty.examples.http.sse;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public final class HttpSseServer {

    static final int DEFAULT_PORT = 8096;
    static final int DEFAULT_INTERVAL = 1000;

    private final int port;
    private final int interval;

    public HttpSseServer(int port, int interval) {
        this.port = port;
        this.interval = interval;
    }

    public HttpServer<ByteBuf, ServerSentEvent> createServer() {
        HttpServer<ByteBuf, ServerSentEvent> server = RxNetty.createHttpServer(port,
                new RequestHandler<ByteBuf, ServerSentEvent>() {
                    @Override
                    public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                   HttpServerResponse<ServerSentEvent> response) {
                        return getIntervalObservable(response);
                    }
                }, PipelineConfigurators.<ByteBuf>sseServerConfigurator());
        System.out.println("HTTP Server Sent Events server started...");
        return server;
    }

    private Observable<Void> getIntervalObservable(final HttpServerResponse<ServerSentEvent> response) {
        return Observable.interval(interval, TimeUnit.MILLISECONDS)
                .flatMap(new Func1<Long, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Long interval) {
                        System.out.println("Writing SSE event for interval: " + interval);
                        return response.writeAndFlush(new ServerSentEvent(String.valueOf(interval), "notification", "hello " + interval));
                    }
                }).materialize()
                .takeWhile(new Func1<Notification<Void>, Boolean>() {
                    @Override
                    public Boolean call(Notification<Void> notification) {
                        if (notification.isOnError()) {
                            System.out.println("Write to client failed, stopping response sending.");
                            notification.getThrowable().printStackTrace(System.err);
                        }
                        return !notification.isOnError();
                    }
                })
                .map(new Func1<Notification<Void>, Void>() {
                    @Override
                    public Void call(Notification<Void> notification) {
                        return null;
                    }
                });
    }

    public static void main(String[] args) {
        new HttpSseServer(DEFAULT_PORT, DEFAULT_INTERVAL).createServer().startAndWait();
    }
}
