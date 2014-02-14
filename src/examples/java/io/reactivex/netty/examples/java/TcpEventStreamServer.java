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

import io.reactivex.netty.ConnectionHandler;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Notification;
import rx.Observable;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public final class TcpEventStreamServer {

    public static void main(String[] args) {
        RxNetty.createTcpServer(8181, PipelineConfigurators.textOnlyConfigurator(),
                                new ConnectionHandler<String, String>() {
                                    @Override
                                    public Observable<Void> handle(ObservableConnection<String, String> newConnection) {
                                        return startEventStream(newConnection);
                                    }
                                }).startAndWait();
    }

    private static Observable<Void> startEventStream(final ObservableConnection<String, String> connection) {
        return Observable.interval(10, TimeUnit.MILLISECONDS)
                         .flatMap(new Func1<Long, Observable<Notification<Void>>>() {
                                     @Override
                                     public Observable<Notification<Void>> call(
                                             Long interval) {
                                         System.out.println(
                                                 "Writing event: "
                                                 + interval);
                                         return connection.writeAndFlush(
                                                 "data: {\"type\":\"Command\",\"name\":\"GetAccount\",\"currentTime\":1376957348166,\"errorPercentage\":0,\"errorCount\":0,\"requestCount\":"
                                                 + interval + "}\n")
                                                          .materialize();
                                     }
                                 })
                         .takeWhile(new Func1<Notification<Void>, Boolean>() {
                                     @Override
                                     public Boolean call(
                                             Notification<Void> notification) {
                                         return !notification
                                                 .isOnError();
                                     }
                                 })
                         .finallyDo(new Action0() {
                                    @Override
                                    public void call() {
                                        System.out.println(" --> Closing connection and stream");
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
