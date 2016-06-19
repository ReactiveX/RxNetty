/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.protocol.tcp.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.test.util.MockConnectionEventListener.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.net.SocketAddress;

public class EventListenerTest {

    @Rule
    public final TcpServerRule rule = new TcpServerRule();

    @Test(timeout = 60000)
    public void testEventListener() throws Exception {
        TcpClient<ByteBuf, ByteBuf> client = TcpClient.newClient(rule.serverAddress);

        assertListenerCalled(client);
    }

    @Test(timeout = 60000)
    public void testEventListenerPostCopy() throws Exception {
        TcpClient<ByteBuf, ByteBuf> client = TcpClient.newClient(rule.serverAddress)
                                                      .enableWireLogging("test", LogLevel.ERROR);

        assertListenerCalled(client);
    }

    @Test(timeout = 60000)
    public void testSubscriptionPreCopy() throws Exception {
        TcpClient<ByteBuf, ByteBuf> client = TcpClient.newClient(rule.serverAddress);

        MockTcpClientEventListener listener = subscribe(client);

        client = client.enableWireLogging("test", LogLevel.DEBUG);

        connectAndAssertListenerInvocation(client, listener);
    }

    private static void assertListenerCalled(TcpClient<ByteBuf, ByteBuf> client) {
        MockTcpClientEventListener listener = subscribe(client);
        connectAndAssertListenerInvocation(client, listener);
    }

    private static void connectAndAssertListenerInvocation(TcpClient<ByteBuf, ByteBuf> client,
                                                           MockTcpClientEventListener listener) {
        TestSubscriber<ByteBuf> subscriber = new TestSubscriber<>();
        client.createConnectionRequest().flatMap(new Func1<Connection<ByteBuf, ByteBuf>, Observable<ByteBuf>>() {
            @Override
            public Observable<ByteBuf> call(Connection<ByteBuf, ByteBuf> c) {
                return c.getInput();
            }
        }).take(1).subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        listener.assertMethodsCalled(Event.BytesRead);
    }

    private static MockTcpClientEventListener subscribe(TcpClient<ByteBuf, ByteBuf> client) {
        MockTcpClientEventListener listener = new MockTcpClientEventListener();
        client.subscribe(listener);
        return listener;
    }

    public static class TcpServerRule extends ExternalResource {

        private SocketAddress serverAddress;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    serverAddress = TcpServer.newServer().start(new ConnectionHandler<ByteBuf, ByteBuf>() {
                        @Override
                        public Observable<Void> handle(Connection<ByteBuf, ByteBuf> newConnection) {
                            return newConnection.writeString(Observable.just("Hello"));
                        }
                    }).getServerAddress();
                    base.evaluate();
                }
            };
        }
    }

}
