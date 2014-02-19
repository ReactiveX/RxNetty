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
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpRequest;
import io.reactivex.netty.protocol.http.server.HttpResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Notification;
import rx.Observable;
import rx.util.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public final class HttpSseServer {

    public static void main(String[] args) {
        final int port = 8080;

        RxNetty.createHttpServer(port,
                                 new RequestHandler<ByteBuf, ServerSentEvent>() {
                                     @Override
                                     public Observable<Void> handle(HttpRequest<ByteBuf> request,
                                                                    HttpResponse<ServerSentEvent> response) {
                                         return getIntervalObservable(response);
                                     }
                                 }, PipelineConfigurators.<ByteBuf>sseServerConfigurator()).startAndWait();
    }

    private static Observable<Void> getIntervalObservable(final HttpResponse<ServerSentEvent> response) {
        return Observable.interval(1, TimeUnit.SECONDS)
                         .flatMap(new Func1<Long, Observable<Notification<Void>>>() {
                             @Override
                             public Observable<Notification<Void>> call(Long interval) {
                                 System.out.println("Writing SSE event for interval: " + interval);
                                 return response.writeAndFlush(new ServerSentEvent("1", "data: ", String.valueOf(
                                         interval))).materialize();
                             }
                         })
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
}
