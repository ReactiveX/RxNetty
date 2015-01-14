/*
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.examples.http.websocket;

import static io.reactivex.netty.examples.http.websocket.WebSocketHelloServer.DEFAULT_PORT;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.websocket.WebSocketClient;

import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observable;

public class WebSocketHelloClient extends ExamplesEnvironment {

    static final int DEFAULT_NO_OF_EVENTS = 100;
    static final int DEFAULT_INTERVAL = 100;

    private final int port;

    public WebSocketHelloClient(int port) {
        this.port = port;
    }

    public void sendHelloRequests(final int noOfEvents, final int interval) throws Exception {
        WebSocketClient<TextWebSocketFrame, TextWebSocketFrame> rxClient =
                RxNetty.<TextWebSocketFrame, TextWebSocketFrame> newWebSocketClientBuilder("localhost", port)
                        .withWebSocketURI("/websocket")
                        .withWebSocketVersion(WebSocketVersion.V13)
                        .build();

        rxClient.connect()
                .flatMap((ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> connection) -> {
                    // start ping-pong session with an initial write
                    return connection.writeAndFlush(new TextWebSocketFrame("ping"))
                            // then we start reading
                            .concatWith(connection.getInput()
                                    // until a certain number of messages have been received
                                    .take(noOfEvents)
                                    // and for each message we log it and response back after a delay
                                    .flatMap((TextWebSocketFrame webSocketFrame) -> {
                                        System.out.println("Got back: " + webSocketFrame.text());
                                        // ping-pong back "ping" after 'internal' milliseconds
                                        return Observable.timer(interval, TimeUnit.MILLISECONDS)
                                                .flatMap(aLong -> connection.writeAndFlush(new TextWebSocketFrame("ping")));
                                    }));
                })
                .toBlocking() // block and wait for terminal event after `noOfEvents` is received and we unsubscribe
                .last();
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new WebSocketHelloClient(port).sendHelloRequests(DEFAULT_NO_OF_EVENTS, DEFAULT_INTERVAL);
    }
}
