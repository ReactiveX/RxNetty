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

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tomasz Bak
 */
public class WebSocketHelloTest {

    private static final int INTERVAL = 1;
    private static final int NO_OF_EVENTS = 3;

    private RxServer<WebSocketFrame, WebSocketFrame> server;

    @Before
    public void setupServer() {
        server = new WebSocketHelloServer(0).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testRequestReplySequence() throws Exception {
        WebSocketHelloClient client = new WebSocketHelloClient(server.getServerPort());
        client.sendHelloRequests(NO_OF_EVENTS, INTERVAL);
    }
}