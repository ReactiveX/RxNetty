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