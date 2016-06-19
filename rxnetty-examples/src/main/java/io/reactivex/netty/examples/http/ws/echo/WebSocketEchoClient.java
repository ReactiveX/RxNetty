/*
 * Copyright 2015 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.examples.http.ws.echo;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import org.slf4j.Logger;
import rx.Observable;

import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * An example to demonstrate how to write a WebSocket client. This example sends a stream of 10 websocket frames to the
 * specified server and expects 10 websocket frames in response.
 *
 * There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server ({@link WebSocketEchoServer})
 * on an ephemeral port and then send an HTTP request to that server and print the response.
 *
 * <h2>After starting {@link WebSocketEchoServer}</h2>
 *
 * If you want to see how {@link WebSocketEchoServer} work, you can run {@link WebSocketEchoServer} by yourself and
 * then pass the port on which the server started to this class as a program argument:
 *
 <PRE>
    java io.reactivex.netty.examples.http.ws.echo.WebSocketEchoClient [server port]
 </PRE>
 *
 * <h2>Existing HTTP server</h2>
 *
 * You can also use this client to send a GET request "/ws" to an existing HTTP server (different than
 * {@link WebSocketEchoServer}) by passing the port and host of the existing server similar to the case above:
 *
 <PRE>
 java io.reactivex.netty.examples.http.ws.echo.WebSocketEchoClient [server port]
 </PRE>
 * If the server host is omitted from the above, it defaults to "127.0.0.1"
 *
 * In all the above usages, this client will print the response received from the server.
 */
public final class WebSocketEchoClient {

    public static void main(String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(WebSocketEchoClient.class);
        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress socketAddress = env.getServerAddress(WebSocketEchoServer.class, args);
        Logger logger = env.getLogger();

        HttpClient.newClient(socketAddress)
                  .enableWireLogging("ws-echo-client", LogLevel.DEBUG)
                  .createGet("/ws")
                  .requestWebSocketUpgrade()
                  .doOnNext(resp -> logger.info(resp.toString()))
                  .flatMap(WebSocketResponse::getWebSocketConnection)
                  .flatMap(conn -> conn.write(Observable.range(1, 10)
                                                        .map(anInt -> new TextWebSocketFrame("Interval " + anInt))
                                                        .cast(WebSocketFrame.class)
                                                        .concatWith(Observable.just(new CloseWebSocketFrame())))
                                       .cast(WebSocketFrame.class)
                                       .mergeWith(conn.getInput())
                  )
                  .take(10)
                  .toBlocking()
                  .forEach(frame -> logger.info(frame.content().toString(Charset.defaultCharset())));
    }
}
