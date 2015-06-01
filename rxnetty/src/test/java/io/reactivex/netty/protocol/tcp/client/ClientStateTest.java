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
package io.reactivex.netty.protocol.tcp.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import io.reactivex.netty.protocol.tcp.client.internal.TcpEventPublisherFactory;
import io.reactivex.netty.protocol.tcp.internal.LoggingHandlerFactory;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Matchers;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
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
                                                              .channelOption(ChannelOption.MAX_MESSAGES_PER_READ, 100);

        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options not copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(not(newState.unsafeChannelOptions())));
        assertThat("Detached pipeline copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(newState.unsafeDetachedPipeline()));

        ClientState<String, String> oldState = clientStateRule.updateState(newState);

        Channel channel = clientStateRule.connect();
        assertThat("Channel option not set in the channel.", channel.config().getMaxMessagesPerRead(), is(100));

        Channel oldStateChannel = clientStateRule.connect(oldState);
        assertThat("Channel option updated in the old state.", oldStateChannel.config().getMaxMessagesPerRead(),
                   is(not(100)));
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirst() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();

        ClientState<String, String> newState = clientStateRule.clientState
                                                              .addChannelHandlerFirst(handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addFirst(handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirstWithEventExecGroup() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        NioEventLoopGroup executor = new NioEventLoopGroup();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerFirst(executor, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addFirst(executor, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLast() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerLast(handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addLast(handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLastWithEventExecGroup() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        NioEventLoopGroup executor = new NioEventLoopGroup();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerLast(executor, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addLast(executor, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerBefore() throws Exception {
        String handlerName = "test_handler";
        String baseHandlerName = "test_handler_base";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerBefore(baseHandlerName, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addBefore(baseHandlerName, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerBeforeWithEventExecGroup() throws Exception {
        String handlerName = "test_handler";
        String baseHandlerName = "test_handler_base";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        NioEventLoopGroup executor = new NioEventLoopGroup();
        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerBefore(executor, baseHandlerName, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addBefore(executor, baseHandlerName, handlerName,
                                                                    handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerAfter() throws Exception {
        String handlerName = "test_handler";
        String baseHandlerName = "test_handler_base";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerAfter(baseHandlerName, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addAfter(baseHandlerName, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);

    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerAfterWithEventExecGroup() throws Exception {
        String handlerName = "test_handler";
        String baseHandlerName = "test_handler_base";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        NioEventLoopGroup executor = new NioEventLoopGroup();
        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerAfter(executor, baseHandlerName, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addAfter(executor, baseHandlerName, handlerName,
                                                                   handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);

    }

    @Test(timeout = 60000)
    public void testPipelineConfigurator() throws Exception {
        final Action1<ChannelPipeline> pipelineConfigurator = new Action1<ChannelPipeline>() {
            @Override
            public void call(ChannelPipeline pipeline) {
            }
        };

        ClientState<String, String> newState = clientStateRule.clientState.pipelineConfigurator(pipelineConfigurator);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).configure(pipelineConfigurator);

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testEnableWireLogging() throws Exception {
        ClientState<String, String> newState = clientStateRule.clientState.enableWireLogging(LogLevel.ERROR);

        clientStateRule.verifyMockPipelineAccessPostCopy();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Options copied.", clientStateRule.clientState.unsafeChannelOptions(),
                   is(newState.unsafeChannelOptions()));
        assertThat("Detached pipeline not copied.", clientStateRule.clientState.unsafeDetachedPipeline(),
                   is(not(newState.unsafeDetachedPipeline())));

        Mockito.verify(newState.unsafeDetachedPipeline()).addFirst(HandlerNames.WireLogging.getName(),
                                                                   LoggingHandlerFactory.getFactory(LogLevel.ERROR));

        Mockito.verifyNoMoreInteractions(newState.unsafeDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
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
                                          .enableWireLogging(LogLevel.ERROR)
                                          .start(new ConnectionHandler<ByteBuf, ByteBuf>() {
                                              @Override
                                              public Observable<Void> handle(
                                                      Connection<ByteBuf, ByteBuf> newConnection) {
                                                  return newConnection.writeString(Observable.just("Hello"));
                                              }
                                          });
                    InetSocketAddress address = new InetSocketAddress("localhost", mockServer.getServerPort());
                    clientState = ClientState.create(mockPipeline, new TcpEventPublisherFactory(),
                                                     ConnectionProvider.<String, String>forHost(address))
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

            final ChannelFuture connect = state.newBootstrap(new ListenersHolder<TcpClientEventListener>())
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

        public Func0<ChannelHandler> newHandler() {
            return new Func0<ChannelHandler>() {
                @Override
                public ChannelHandler call() {
                    return new TestableChannelHandler();
                }
            };
        }

        public void assertHandlerExists(String errMsg, ChannelHandler handler) {
            assertThat(errMsg, handler, is(notNullValue()));
            assertThat(errMsg, handler, is(instanceOf(TestableChannelHandler.class)));
        }

        public void verifyMockPipelineAccessPostCopy() {
            Mockito.verify(mockPipeline).copy(Matchers.<Action1<ChannelPipeline>>anyObject());
        }

        public ClientState<String, String> updateState(ClientState<String, String> newState) {
            final ClientState<String, String> current = clientState;
            clientState = newState;
            return current;
        }

        public static class TestableChannelHandler extends ChannelDuplexHandler {
        }
    }
}