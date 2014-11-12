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
package io.reactivex.netty.examples.http.websocket;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.websocket.WebSocketClient;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.examples.http.websocket.WebSocketHelloServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class WebSocketHelloClient extends ExamplesEnvironment {

    static final int DEFAULT_NO_OF_EVENTS = 100;
    static final int DEFAULT_INTERVAL = 100;

    private final int port;

    public WebSocketHelloClient(int port) {
        this.port = port;
    }

    public void sendHelloRequests(final int noOfEvents, final int interval) throws Exception {
        WebSocketClient<TextWebSocketFrame, TextWebSocketFrame> rxClient =
                RxNetty.<TextWebSocketFrame, TextWebSocketFrame>newWebSocketClientBuilder("localhost", port)
                        .withWebSocketURI("/websocket")
                        .withWebSocketVersion(WebSocketVersion.V13)
                        .build();

        Notification<Void> result = rxClient.connect()
                .flatMap(new Func1<ObservableConnection<TextWebSocketFrame, TextWebSocketFrame>, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(final ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> connection) {
                        return Observable.concat(
                                connection.writeAndFlush(new TextWebSocketFrame("Hello!!!")),
                                connection.getInput().take(noOfEvents).flatMap(new Func1<TextWebSocketFrame, Observable<Void>>() {
                                    @Override
                                    public Observable<Void> call(TextWebSocketFrame webSocketFrame) {
                                        System.out.println("Got back: " + webSocketFrame.text());
                                        return Observable.timer(interval, TimeUnit.MILLISECONDS)
                                                .flatMap(new Func1<Long, Observable<Void>>() {
                                                    @Override
                                                    public Observable<Void> call(Long aLong) {
                                                        return connection.writeAndFlush(new TextWebSocketFrame("Hello!!!"));
                                                    }
                                                });
                                    }
                                }));
                    }
                }).materialize().toBlocking().last();

        if (result.isOnError()) {
            throw (Exception) result.getThrowable();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new WebSocketHelloClient(port).sendHelloRequests(DEFAULT_NO_OF_EVENTS, DEFAULT_INTERVAL);
    }
}
