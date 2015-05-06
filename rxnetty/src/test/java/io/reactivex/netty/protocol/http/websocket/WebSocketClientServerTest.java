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
package io.reactivex.netty.protocol.http.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.server.RxServer;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Tomasz Bak
 */
public class WebSocketClientServerTest {

    @Test
    public void testTextCommunication() throws Exception {
        TestSequenceExecutor executor = new TestSequenceExecutor()
                .withClientFrames(new TextWebSocketFrame("clientRequest"))
                .withExpectedOnServer(1)
                .withServerFrames(new TextWebSocketFrame("serverResponse"))
                .withExpectedOnClient(1)
                .execute();

        assertEquals("Expected original client request", "clientRequest", asText(executor.getReceivedClientFrames().get(0)));
        assertEquals("Expected original server response", "serverResponse", asText(executor.getReceivedServerFrames().get(0)));
    }

    @Test
    public void testBinaryCommunication() throws Exception {
        TestSequenceExecutor executor = new TestSequenceExecutor()
                .withClientFrames(new BinaryWebSocketFrame(toByteBuf("clientRequest")))
                .withExpectedOnServer(1)
                .withServerFrames(new BinaryWebSocketFrame(toByteBuf("serverResponse")))
                .withExpectedOnClient(1)
                .execute();

        assertEquals("Expected original client request", "clientRequest", asText(executor.getReceivedClientFrames().get(0)));
        assertEquals("Expected original server response", "serverResponse", asText(executor.getReceivedServerFrames().get(0)));
    }

    @Test
    public void testFragmentedMessage() throws Exception {
        TestSequenceExecutor executor = new TestSequenceExecutor()
                .withClientFrames(
                        new TextWebSocketFrame(false, 0, "first"),
                        new ContinuationWebSocketFrame(false, 0, "middle"),
                        new ContinuationWebSocketFrame(true, 0, "last")
                )
                .withExpectedOnServer(3)
                .execute();

        assertEquals("Expected first frame content", "first", asText(executor.getReceivedClientFrames().get(0)));
        assertEquals("Expected first frame content", "middle", asText(executor.getReceivedClientFrames().get(1)));
        assertEquals("Expected first frame content", "last", asText(executor.getReceivedClientFrames().get(2)));
    }

    @Test
    public void testMessageAggregationOnServer() throws Exception {
        TestSequenceExecutor executor = new TestSequenceExecutor()
                .withMessageAggregation(true)
                .withClientFrames(new TextWebSocketFrame(false, 0, "0123456789"), new ContinuationWebSocketFrame(true, 0, "ABCDEFGHIJ"))
                .withExpectedOnServer(1)
                .execute();
        assertEquals("Expected aggregated message", "0123456789ABCDEFGHIJ", asText(executor.getReceivedClientFrames().get(0)));
    }

    @Test
    public void testMessageAggregationOnClient() throws Exception {
        TestSequenceExecutor executor = new TestSequenceExecutor()
                .withMessageAggregation(true)
                .withServerFrames(new TextWebSocketFrame(false, 0, "0123456789"), new ContinuationWebSocketFrame(true, 0, "ABCDEFGHIJ"))
                .withExpectedOnClient(1)
                .execute();
        assertEquals("Expected aggregated message", "0123456789ABCDEFGHIJ", asText(executor.getReceivedServerFrames().get(0)));
    }

    @Test
    public void testPingPong() throws Exception {
        TestSequenceExecutor executor = new TestSequenceExecutor()
                .withClientFrames(new PingWebSocketFrame())
                .withExpectedOnServer(1)
                .withServerFrames(new PongWebSocketFrame())
                .withExpectedOnClient(1)
                .execute();

        assertTrue("Expected ping on server", executor.getReceivedClientFrames().get(0) instanceof PingWebSocketFrame);
        assertTrue("Expected pong on client", executor.getReceivedServerFrames().get(0) instanceof PongWebSocketFrame);
    }

    @Test
    public void testConnectionClose() throws Exception {
        TestSequenceExecutor executor = new TestSequenceExecutor()
                .withClientFrames(new CloseWebSocketFrame(1000, "close"))
                .withExpectedOnServer(1)
                .withServerFrames(new CloseWebSocketFrame(1001, "close requested"))
                .withExpectedOnClient(1)
                .execute();

        assertTrue("Expected close on server", executor.getReceivedClientFrames().get(0) instanceof CloseWebSocketFrame);
        assertTrue("Expected close on server", executor.getReceivedServerFrames().get(0) instanceof CloseWebSocketFrame);
    }

    private static ByteBuf toByteBuf(String text) {
        byte[] bytes = text.getBytes(Charset.defaultCharset());
        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(bytes.length);
        return byteBuf.writeBytes(bytes);
    }

    private static String asText(WebSocketFrame frame) {
        return frame.content().toString(Charset.defaultCharset());
    }

    private static class TestSequenceExecutor {
        private WebSocketFrame[] clientFrames;
        private int expectedOnServer;
        private WebSocketFrame[] serverFrames;
        private int expectedOnClient;

        private final List<WebSocketFrame> receivedClientFrames = new CopyOnWriteArrayList<WebSocketFrame>();
        private final List<WebSocketFrame> receivedServerFrames = new CopyOnWriteArrayList<WebSocketFrame>();
        private boolean messageAggregation;

        public List<WebSocketFrame> getReceivedClientFrames() {
            return receivedClientFrames;
        }

        public List<WebSocketFrame> getReceivedServerFrames() {
            return receivedServerFrames;
        }

        public TestSequenceExecutor withMessageAggregation(boolean messageAggregation) {
            this.messageAggregation = messageAggregation;
            return this;
        }

        public TestSequenceExecutor withClientFrames(WebSocketFrame... clientFrames) {
            this.clientFrames = clientFrames;
            return this;
        }

        public TestSequenceExecutor withExpectedOnServer(int expectedOnServer) {
            this.expectedOnServer = expectedOnServer;
            return this;
        }

        public TestSequenceExecutor withServerFrames(WebSocketFrame... serverFrames) {
            this.serverFrames = serverFrames;
            return this;
        }

        public TestSequenceExecutor withExpectedOnClient(int expectedOnClient) {
            this.expectedOnClient = expectedOnClient;
            return this;
        }

        public TestSequenceExecutor execute() throws InterruptedException, TimeoutException, ExecutionException {
            final CountDownLatch serverLatch = new CountDownLatch(expectedOnServer);
            RxServer<WebSocketFrame, WebSocketFrame> server = RxNetty.newWebSocketServerBuilder(0, new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
                @Override
                public Observable<Void> handle(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
                    if (clientFrames == null) {
                        return sendBatchOfFrames(connection, serverFrames);
                    }
                    return connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                        @Override
                        public Observable<Void> call(WebSocketFrame frame) {
                            frame.retain();
                            receivedClientFrames.add(frame);
                            serverLatch.countDown();
                            if (serverLatch.getCount() == 0) {
                                return sendBatchOfFrames(connection, serverFrames);
                            }
                            return Observable.empty();
                        }
                    });
                }
            }).withMessageAggregator(messageAggregation)./*enableWireLogging(LogLevel.ERROR).*/build().start();

            final CountDownLatch clientLatch = new CountDownLatch(expectedOnClient);
            RxNetty.newWebSocketClientBuilder("localhost", server.getServerPort())
                    .withWebSocketVersion(WebSocketVersion.V13)
                    .withMessageAggregation(messageAggregation)
                    .enableWireLogging(LogLevel.ERROR)
                    .build()
                    .connect()
                    .flatMap(new Func1<ObservableConnection<WebSocketFrame, WebSocketFrame>, Observable<WebSocketFrame>>() {
                        @Override
                        public Observable<WebSocketFrame> call(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
                            sendBatchOfFrames(connection, clientFrames);
                            return connection.getInput().doOnNext(new Action1<WebSocketFrame>() {
                                @Override
                                public void call(WebSocketFrame webSocketFrame) {
                                    webSocketFrame.retain();
                                }
                            });
                        }
                    })
                    .subscribe(new Action1<WebSocketFrame>() {
                        @Override
                        public void call(WebSocketFrame webSocketFrame) {
                            receivedServerFrames.add(webSocketFrame);
                            clientLatch.countDown();
                        }
                    });
            assertTrue("Timeout on server", serverLatch.await(30, TimeUnit.SECONDS));
            assertTrue("Timeout on client", clientLatch.await(30, TimeUnit.SECONDS));

            server.shutdown();

            assertEquals("Invalid number of server frames received", expectedOnClient, receivedServerFrames.size());
            assertEquals("Invalid number of client frames received", expectedOnServer, receivedClientFrames.size());

            return this;
        }

        private static Observable<Void> sendBatchOfFrames(ObservableConnection<WebSocketFrame, WebSocketFrame> connection,
                                                          WebSocketFrame[] frames) {
            if (frames != null) {
                for (WebSocketFrame frame : frames) {
                    connection.write(frame);
                }
                return connection.flush();
            }
            return Observable.empty();
        }
    }
}