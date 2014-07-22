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

package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.ChannelCloseListener;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.ErrorHandler;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

/**
 * @author Nitesh Kant
 */
public class UnexpectedErrorsTest {

    public int port;
    private RxServer<ByteBuf,ByteBuf> server;
    private final ChannelCloseListener channelCloseListener = new ChannelCloseListener();

    @Before
    public void setUp() throws Exception {
        server = RxNetty.createTcpServer(0, new PipelineConfigurator<ByteBuf, ByteBuf>() {
                                             @Override
                                             public void configureNewPipeline(ChannelPipeline pipeline) {
                                                 pipeline.addLast(channelCloseListener);
                                             }
                                         },
                                         new ConnectionHandler<ByteBuf, ByteBuf>() {
                                             @Override
                                             public Observable<Void> handle(
                                                     ObservableConnection<ByteBuf, ByteBuf> newConnection) {
                                                 return Observable.error(new IllegalStateException(
                                                         "I always throw an error."));
                                             }
                                         });
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        server.waitTillShutdown(1, TimeUnit.MINUTES);
    }

    @Test
    public void testErrorHandlerReturnsNull() throws Exception {
        TestableErrorHandler errorHandler = new TestableErrorHandler(null);
        server.withErrorHandler(errorHandler).start();
        System.err.println("[testErrorHandlerReturnsNull] Server port: " + server.getServerPort());
        blockTillConnected(server.getServerPort());
        channelCloseListener.waitForClose(1, TimeUnit.MINUTES);

        assertTrue("Error handler not invoked.", errorHandler.invoked);
    }

    @Test
    public void testConnectionHandlerReturnsError() throws Exception {
        TestableErrorHandler errorHandler = new TestableErrorHandler(
                Observable.<Void>error(new IllegalStateException("I always throw an error.")));

        server.withErrorHandler(errorHandler).start();

        System.err.println("[testConnectionHandlerReturnsError] Server port: " + server.getServerPort());

        blockTillConnected(server.getServerPort());

        channelCloseListener.waitForClose(1, TimeUnit.MINUTES);

        assertTrue("Error handler not invoked.", errorHandler.invoked);
    }

    private static void blockTillConnected(int serverPort)
            throws ExecutionException, InterruptedException, TimeoutException {
        ObservableConnection<ByteBuf, ByteBuf> conn = RxNetty.createTcpClient("localhost", serverPort).connect()
                                                                  .toBlocking().toFuture().get(1, TimeUnit.MINUTES);
        conn.close();
    }


    private static class TestableErrorHandler implements ErrorHandler {

        private final Observable<Void> toReturn;
        private boolean invoked;

        private TestableErrorHandler(Observable<Void> toReturn) {
            this.toReturn = toReturn;
        }

        @Override
        public Observable<Void> handleError(Throwable throwable) {
            invoked = true;
            return toReturn;
        }
    }
}
