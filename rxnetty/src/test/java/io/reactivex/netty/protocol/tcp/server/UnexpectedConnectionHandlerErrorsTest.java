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
 */

package io.reactivex.netty.protocol.tcp.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class UnexpectedConnectionHandlerErrorsTest {

    @Rule
    public final ErrorRule rule = new ErrorRule();

    @Test(timeout = 60000)
    public void testHandlerReturnsNull() throws Exception {
        rule.server.start(new ConnectionHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(Connection<ByteBuf, ByteBuf> newConnection) {
                return null;
            }
        });

        connectAndWaitForClose();
    }

    @Test(timeout = 60000)
    public void testHandlerReturnsError() throws Exception {
        rule.server.start(new ConnectionHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(Connection<ByteBuf, ByteBuf> newConnection) {
                return Observable.error(new IllegalStateException());
            }
        });

        connectAndWaitForClose();
    }

    @Test(timeout = 60000)
    public void testHandlerThrowsError() throws Exception {
        rule.server.start(new ConnectionHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(Connection<ByteBuf, ByteBuf> newConnection) {
                return Observable.error(new IllegalStateException());
            }
        });

        connectAndWaitForClose();
    }

    private void connectAndWaitForClose() {
        Connection<ByteBuf, ByteBuf> connection = rule.connectToServer();

        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        connection.closeListener().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();
    }

    public static class ErrorRule extends ExternalResource {

        private TcpServer<ByteBuf, ByteBuf> server;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    server = TcpServer.newServer(0);
                    base.evaluate();
                }
            };
        }

        public Connection<ByteBuf, ByteBuf> connectToServer() {
            final TestSubscriber<Connection<ByteBuf, ByteBuf>> subscriber = new TestSubscriber<>();

            TcpClient.newClient("127.0.0.1", server.getServerPort())
                     .channelOption(ChannelOption.AUTO_READ, true) /*Else nothing is read from the channel even close*/
                     .createConnectionRequest()
                     .enableWireLogging(LogLevel.ERROR)
                     .subscribe(subscriber);

            subscriber.awaitTerminalEvent();
            subscriber.assertNoErrors();

            assertThat("No connection available.", subscriber.getOnNextEvents(), hasSize(1));

            return subscriber.getOnNextEvents().get(0);
        }
    }
}
