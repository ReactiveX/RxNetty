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
package rx.netty.examples

import java.util.concurrent.TimeUnit

import rx.Notification
import rx.Observable
import rx.netty.RxNetty
import rx.netty.impl.ObservableConnection
import rx.netty.protocol.tcp.ProtocolHandlers

/**
 * When a client connects it will start emitting an infinite stream of events.
 */
class TcpEventStreamServer {

    public static void main(String[] args) {
        createServer(8181).toBlockingObservable().last();
    }

    public static Observable<String> createServer(final int port) {
        return RxNetty.createTcpServer(port, ProtocolHandlers.stringLineCodec())
            .onConnect({ ObservableConnection<String, String> connection ->
                connection.write("Hello!\n");
                return getEventStream(connection).subscribe({});
            }).startAndAwait();
    }

    public static Observable<Void> getEventStream(final ObservableConnection<String, String> connection) {
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