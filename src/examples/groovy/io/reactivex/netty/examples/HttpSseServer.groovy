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

import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.reactivex.netty.RxNetty
import io.reactivex.netty.channel.ObservableConnection
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.text.sse.SSEEvent
import rx.Notification
import rx.util.functions.Action1

import java.util.concurrent.TimeUnit

class HttpSseServer {

    public static void main(String[] args) {
        HttpServer<FullHttpRequest, Object> server = RxNetty.createSseServer(8080, null)
        server.start(new Action1<ObservableConnection<FullHttpRequest, Object>>() {
            @Override
            void call(ObservableConnection<FullHttpRequest, Object> connection) {
                connection.getInput().subscribe({ FullHttpRequest httpRequest ->
                    System.out.println("New request recieved: " + httpRequest);
                    connection.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                    getIntervalObservable(connection).subscribe();
                })
            }
        });

        server.waitTillShutdown();

    }

    public static rx.Observable<Void> getIntervalObservable(final ObservableConnection<FullHttpRequest, Object> connection) {
        return rx.Observable.interval(1000, TimeUnit.MILLISECONDS)
                .flatMap({ Long interval ->
                    println("Writing SSE event for interval: " + interval);
                    // emit the interval to the output and return the notification received from it
                    return connection.write(new SSEEvent("1", "data: ", String.valueOf(interval))).materialize();
                }).takeWhile({ Notification<Void> n ->
                    // unsubscribe from interval if we receive an error
                    return !n.isOnError();
                })
    }
}
