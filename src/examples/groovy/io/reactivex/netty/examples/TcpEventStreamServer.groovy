/**
 * Copyright 2013 Netflix, Inc.
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

import io.reactivex.netty.ObservableConnection
import io.reactivex.netty.RxNetty
import io.reactivex.netty.pipeline.PipelineConfigurators
import io.reactivex.netty.server.RxServer
import rx.Notification
import rx.Observable
import rx.util.functions.Action1

import java.util.concurrent.TimeUnit

/**
 * When a client connects it will start emitting an infinite stream of events.
 */
class TcpEventStreamServer {

    public static void main(String[] args) {
        RxServer<String, String> tcpServer = RxNetty.createTcpServer(8181, PipelineConfigurators.textOnlyConfigurator(), null);
        tcpServer.start(new Action1<ObservableConnection<String, String>>() {
            @Override
            public void call(ObservableConnection<String, String> connection) {
                startEventStream(connection).subscribe();
            }
        });

        tcpServer.waitTillShutdown();
    }

    public static Observable<Void> startEventStream(final ObservableConnection<String, String> connection) {
        return Observable.interval(10, TimeUnit.MILLISECONDS)
        .flatMap({ Long interval ->
            System.out.println("Writing event: " + interval);
            // emit the interval to the output and return the notification received from it
            return connection.write("data: {\"type\":\"Command\",\"name\":\"GetAccount\",\"currentTime\":1376957348166,\"errorPercentage\":0,\"errorCount\":0,\"requestCount\":" + interval + "}\n").materialize();
        })
        .takeWhile({ Notification<Void> n ->
            // unsubscribe from interval if we receive an error
            return !n.isOnError();
        }).finallyDo({ println(" --> Closing connection and stream") })
    }
}