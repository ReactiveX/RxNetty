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

package io.reactivex.netty.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.test.util.MockEventPublisher;
import io.reactivex.netty.test.util.embedded.EmbeddedConnectionProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import java.net.InetSocketAddress;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ClientStateTest {

    @Rule
    public final ClientStateRule clientStateRule = new ClientStateRule();

    @Test(timeout = 60000)
    public void testChannelOption() throws Exception {
        ClientState<String, String> newState = clientStateRule.clientState
                .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100);

        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options not copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(not(newState.unsafeChannelOptions())));
        assertThat("Detached pipeline copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(newState.unsafeDetachedPipeline()));

        ClientState<String, String> oldState = clientStateRule.updateState(newState);

        Channel channel = clientStateRule.connect();
        assertThat("Channel option not set in the channel.", channel.config().getConnectTimeoutMillis(), is(100));

        Channel oldStateChannel = clientStateRule.connect(oldState);
        assertThat("Channel option updated in the old state.", oldStateChannel.config().getConnectTimeoutMillis(),
                   is(not(100)));
    }

    public static class ClientStateRule extends ExternalResource {

        private ClientState<String, String> clientState;
        private TcpServer<ByteBuf, ByteBuf> mockServer;
        private DetachedChannelPipeline mockPipeline;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    mockPipeline = Mockito.mock(DetachedChannelPipeline.class, Mockito.RETURNS_MOCKS);

                    mockServer = TcpServer.newServer(0)
                                          .enableWireLogging("test", LogLevel.ERROR)
                                          .start(new ConnectionHandler<ByteBuf, ByteBuf>() {
                                              @Override
                                              public Observable<Void> handle(
                                                      Connection<ByteBuf, ByteBuf> newConnection) {
                                                  return newConnection.writeString(Observable.just("Hello"));
                                              }
                                          });
                    EmbeddedConnectionProvider<String, String> ecp = new EmbeddedConnectionProvider<>();
                    clientState = ClientState.create(mockPipeline, ecp.asFactory(), Observable.<Host>empty())
                                             .enableWireLogging(LogLevel.ERROR);
                    base.evaluate();
                }
            };
        }

        public Channel connect() throws InterruptedException {
            return connect(clientState);
        }

        public Channel connect(final ClientState<String, String> state) throws InterruptedException {
            TestSubscriber<Channel> subscriber = new TestSubscriber<>();

            final ChannelFuture connect = state.newBootstrap(MockEventPublisher.disabled(), null)
                                               .connect(new InetSocketAddress("127.0.0.1", mockServer.getServerPort()));

            Observable.create(new OnSubscribe<Channel>() {
                @Override
                public void call(final Subscriber<? super Channel> subscriber) {
                    connect.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                subscriber.onNext(future.channel());
                                subscriber.onCompleted();
                            } else {
                                subscriber.onError(future.cause());
                            }
                        }
                    });
                }
            }).subscribe(subscriber);

            subscriber.awaitTerminalEvent();
            subscriber.assertNoErrors();

            assertThat("No connection returned from connect.", subscriber.getOnNextEvents(), hasSize(1));

            return subscriber.getOnNextEvents().get(0);
        }

        public ClientState<String, String> updateState(ClientState<String, String> newState) {
            final ClientState<String, String> current = clientState;
            clientState = newState;
            return current;
        }
    }
}
