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

package io.reactivex.netty.examples.tcp.interval;

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.RxServer;
import rx.Notification;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public final class TcpIntervalServer {

    static final int DEFAULT_PORT = 8101;

    private final int port;
    private final int interval;

    public TcpIntervalServer(int port, int interval) {
        this.port = port;
        this.interval = interval;
    }

    public RxServer<String, String> createServer() {
        RxServer<String, String> server = RxNetty.createTcpServer(port, PipelineConfigurators.textOnlyConfigurator(),
                new ConnectionHandler<String, String>() {
                    @Override
                    public Observable<Void> handle(final ObservableConnection<String, String> connection) {
                        System.out.println("--- Connection Started ---");

                        final Observable<String> input = connection.getInput().map(
                                new Func1<String, String>() {
                                    @Override
                                    public String call(String s) {
                                        return s.trim();
                                    }
                                });

                        return input.flatMap(new Func1<String, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(String msg) {
                                if (msg.startsWith("subscribe:")) {
                                    System.out.println("-------------------------------------");
                                    System.out.println(
                                            "Received 'subscribe' from client so starting interval ...");
                                    return getIntervalObservable(connection)
                                            .takeUntil(input.filter(new Func1<String, Boolean>() {
                                                @Override
                                                public Boolean call(String s) {
                                                    return "unsubscribe:".equals(s);
                                                }
                                            }));
                                } else if (msg.startsWith("unsubscribe:")) {
                                    // this is here just for verbose logging
                                    System.out.println(
                                            "Received 'unsubscribe' from client so stopping interval (or ignoring if nothing subscribed) ...");
                                    return Observable.empty();
                                } else {
                                    if (!(msg.isEmpty() || "unsubscribe:".equals(msg))) {
                                        connection.writeAndFlush("\nERROR => Unknown command: " + msg
                                                + "\nCommands => subscribe:, unsubscribe:\n");
                                    }
                                    return Observable.empty();
                                }
                            }
                        }).finallyDo(new Action0() {
                            @Override
                            public void call() {
                                System.out.println("--- Connection Closed ---");
                            }
                        });
                    }
                });
        System.out.println("TCP interval server started...");
        return server;
    }

    private Observable<Void> getIntervalObservable(final ObservableConnection<String, String> connection) {
        return Observable.interval(interval, TimeUnit.MILLISECONDS)
                .flatMap(new Func1<Long, Observable<Notification<Void>>>() {
                    @Override
                    public Observable<Notification<Void>> call(
                            Long interval) {
                        System.out.println(
                                "Writing interval: " + interval);
                        return connection.writeAndFlush("interval => " + interval + '\n').materialize();
                    }
                })
                .takeWhile(new Func1<Notification<Void>, Boolean>() {
                    @Override
                    public Boolean call(Notification<Void> notification) {
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
        int interval = 1000;
        if (args.length > 0) {
            interval = Integer.valueOf(args[0]);
        }
        new TcpIntervalServer(DEFAULT_PORT, interval).createServer().startAndWait();
    }
}
