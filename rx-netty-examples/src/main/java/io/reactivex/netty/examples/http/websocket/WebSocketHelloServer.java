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
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.server.RxServer;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author Tomasz Bak
 */
public class WebSocketHelloServer extends ExamplesEnvironment {

    static final int DEFAULT_PORT = 8090;

    private final int port;

    public WebSocketHelloServer(int port) {
        this.port = port;
    }

    public RxServer<WebSocketFrame, WebSocketFrame> createServer() {
        RxServer<WebSocketFrame, WebSocketFrame> server = RxNetty.newWebSocketServerBuilder(port, new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
            @Override
            public Observable<Void> handle(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
                return connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(WebSocketFrame wsFrame) {
                        TextWebSocketFrame textFrame = (TextWebSocketFrame) wsFrame;
                        System.out.println("Got message: " + textFrame.text());
                        return connection.writeAndFlush(new TextWebSocketFrame(textFrame.text().toUpperCase()));
                    }
                });
            }
        }).enableWireLogging(LogLevel.ERROR).build();

        System.out.println("WebSocket server started...");
        return server;
    }

    public static void main(final String[] args) {
        new WebSocketHelloServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
