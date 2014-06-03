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
package io.reactivex.netty.examples

import io.reactivex.netty.RxNetty
import io.reactivex.netty.channel.ObservableConnection
import io.reactivex.netty.pipeline.PipelineConfigurators
import io.reactivex.netty.server.RxServer
import rx.Notification
import rx.Observable
import rx.functions.Action1

import java.util.concurrent.TimeUnit

/**
 * When a client connects and sends "subscribe:" it will start emitting until it receives "unsubscribe:"
 * <p>
 * It will look something like this:
 * <pre>
 * -------------------------------------
 * Received 'subscribe' from client so starting interval ...
 * Writing interval: 0
 * Writing interval: 1
 * Writing interval: 2
 * Writing interval: 3
 * Received 'unsubscribe' from client so stopping interval (or ignoring if nothing subscribed) ...
 * </pre>
 */
class TcpIntervalServer {

    public static void main(String[] args) {
        createServer(8181).toBlocking().last();
    }

    public static Observable<String> createServer(final int port) {
        RxServer<String, String> tcpServer = RxNetty.createTcpServer(8181, PipelineConfigurators.textOnlyConfigurator(), null);
        tcpServer.start(new Action1<ObservableConnection<String, String>>() {
            @Override
            public void call(ObservableConnection<String, String> connection) {

                println("--- Connection Started ---")

                Observable<String> input = connection.getInput().map({ String m ->
                    return m.trim()
                });

                // for each message we receive on the connection
                input.flatMap({ String msg ->
                    if (msg.startsWith("subscribe:")) {
                        System.out.println("-------------------------------------");
                        System.out.println("Received 'subscribe' from client so starting interval ...");
                        return getIntervalObservable(connection).takeUntil(input.filter({ String m -> m.equals("unsubscribe:")}))
                    } else if (msg.startsWith("unsubscribe:")) {
                        // this is here just for verbose logging
                        System.out.println("Received 'unsubscribe' from client so stopping interval (or ignoring if nothing subscribed) ...");
                        return Observable.empty();
                    } else {
                        if (!(msg.isEmpty() || "unsubscribe:".equals(msg))) {
                            connection.writeString("\nERROR => Unknown command: " + msg + "\nCommands => subscribe:, unsubscribe:\n");
                        }
                        return Observable.empty();
                    }

                }).finallyDo({ println("--- Connection Closed ---") }).subscribe({});
            }
        });

        tcpServer.waitTillShutdown();
    }

    public static Observable<Void> getIntervalObservable(final ObservableConnection<String, String> connection) {
        return Observable.interval(1000, TimeUnit.MILLISECONDS)
        .flatMap({ Long interval ->
            System.out.println("Writing interval: " + interval);
            // emit the interval to the output and return the notification received from it
            return connection.writeString("interval => " + interval + "\n").materialize();
        })
        .takeWhile({ Notification<Void> n ->
            // unsubscribe from interval if we receive an error
            return !n.isOnError();
        })
    }
}
