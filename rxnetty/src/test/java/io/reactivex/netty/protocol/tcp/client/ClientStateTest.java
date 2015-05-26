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
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.channel.pool.PoolConfig;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.protocol.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.protocol.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
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
import rx.functions.Action1;
import rx.functions.Func0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ClientStateTest {

    @Rule
    public final ClientStateRule clientStateRule = new ClientStateRule();

    @Test(timeout = 60000)
    public void testChannelOption() throws Exception {
        ClientState<String, String> newState = clientStateRule.clientState
                                                              .channelOption(ChannelOption.AUTO_READ, true);

        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        ClientState<String, String> oldState = clientStateRule.updateState(newState);

        Channel channel = clientStateRule.connect();
        assertThat("Channel option not set in the channel.", channel.config().isAutoRead(), is(true));

        Channel oldStateChannel = clientStateRule.connect(oldState);
        assertThat("Channel option updated in the old state.", oldStateChannel.config().isAutoRead(), is(false));
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirst() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();

        ClientState<String, String> newState = clientStateRule.clientState
                                                              .addChannelHandlerFirst(handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addFirst(handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirstWithEventExecGroup() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        NioEventLoopGroup executor = new NioEventLoopGroup();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerFirst(executor, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addFirst(executor, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLast() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerLast(handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addLast(handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLastWithEventExecGroup() throws Exception {
        String handlerName = "test_handler";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        NioEventLoopGroup executor = new NioEventLoopGroup();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerLast(executor, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addLast(executor, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerBefore() throws Exception {
        String handlerName = "test_handler";
        String baseHandlerName = "test_handler_base";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();

        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerBefore(baseHandlerName, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addBefore(baseHandlerName, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
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

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addBefore(executor, baseHandlerName, handlerName,
                                                                 handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerAfter() throws Exception {
        String handlerName = "test_handler";
        String baseHandlerName = "test_handler_base";
        Func0<ChannelHandler> handlerFactory = clientStateRule.newHandler();
        ClientState<String, String> newState = clientStateRule.clientState
                .addChannelHandlerAfter(baseHandlerName, handlerName, handlerFactory);

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addAfter(baseHandlerName, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
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

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addAfter(executor, baseHandlerName, handlerName, handlerFactory);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
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

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).configure(pipelineConfigurator);

        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testEnableWireLogging() throws Exception {
        ClientState<String, String> newState = clientStateRule.clientState.enableWireLogging(LogLevel.ERROR);

        clientStateRule.verifyMockPipelineAccessPostCopy(newState);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap not copied.", clientStateRule.clientState.getBootstrap(),
                   is(not(newState.getBootstrap())));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        Mockito.verify(newState.getDetachedPipeline()).addFirst(HandlerNames.WireLogging.getName(),
                                                                LoggingHandlerFactory.getFactory(LogLevel.ERROR));


        Mockito.verifyNoMoreInteractions(newState.getDetachedPipeline());
        Mockito.verifyNoMoreInteractions(clientStateRule.mockPipeline);
    }

    @Test(timeout = 60000)
    public void testMaxConnections() throws Exception {

        final PoolConfig<String, String> oldPoolConfig = clientStateRule.clientState.getPoolConfig();
        assertThat("Client state created with pooling", oldPoolConfig, is(nullValue()));

        ClientState<String, String> newState = clientStateRule.clientState.maxConnections(10);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap copied.", clientStateRule.clientState.getBootstrap(), is(newState.getBootstrap()));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));
        assertThat("Pool config not copied", clientStateRule.clientState.getPoolConfig(),
                   is(not(newState.getPoolConfig())));

        assertThat("Max connections not set.", newState.getPoolConfig().getPoolLimitDeterminationStrategy()
                                                       .getAvailablePermits(),
                   is(10));
    }

    @Test(timeout = 60000)
    public void testMaxIdleTimeoutMillis() throws Exception {
        final PoolConfig<String, String> oldPoolConfig = clientStateRule.clientState.getPoolConfig();
        assertThat("Client state created with pooling", oldPoolConfig, is(nullValue()));

        ClientState<String, String> newState = clientStateRule.clientState.maxIdleTimeoutMillis(10000);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap copied.", clientStateRule.clientState.getBootstrap(), is(newState.getBootstrap()));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));
        assertThat("Pool config not copied", clientStateRule.clientState.getPoolConfig(),
                   is(not(newState.getPoolConfig())));

        assertThat("Max idle time not set.", newState.getPoolConfig().getMaxIdleTimeMillis(), is(10000L));
    }

    @Test(timeout = 60000)
    public void testConnectionPoolLimitStrategy() throws Exception {
        final PoolConfig<String, String> oldPoolConfig = clientStateRule.clientState.getPoolConfig();
        assertThat("Client state created with pooling", oldPoolConfig, is(nullValue()));

        PoolLimitDeterminationStrategy strategy = new MaxConnectionsBasedStrategy(100);
        ClientState<String, String> newState = clientStateRule.clientState.connectionPoolLimitStrategy(strategy);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap copied.", clientStateRule.clientState.getBootstrap(), is(newState.getBootstrap()));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));
        assertThat("Pool config not copied", clientStateRule.clientState.getPoolConfig(),
                   is(not(newState.getPoolConfig())));

        assertThat("Connection pool limit strategy not set.",
                   newState.getPoolConfig().getPoolLimitDeterminationStrategy(), is(strategy));
    }

    @Test(timeout = 60000)
    public void testIdleConnectionCleanupTimer() throws Exception {
        final PoolConfig<String, String> oldPoolConfig = clientStateRule.clientState.getPoolConfig();
        assertThat("Client state created with pooling", oldPoolConfig, is(nullValue()));

        Observable<Long> timer = Observable.never();
        ClientState<String, String> newState = clientStateRule.clientState.idleConnectionCleanupTimer(timer);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap copied.", clientStateRule.clientState.getBootstrap(), is(newState.getBootstrap()));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));
        assertThat("Pool config not copied", clientStateRule.clientState.getPoolConfig(),
                   is(not(newState.getPoolConfig())));

        assertThat("Idle connection cleanup timer not set.", newState.getPoolConfig().getIdleConnectionsCleanupTimer(),
                   is(timer));
    }

    @Test(timeout = 60000)
    public void testNoIdleConnectionCleanup() throws Exception {
        final PoolConfig<String, String> oldPoolConfig = clientStateRule.clientState.getPoolConfig();
        assertThat("Client state created with pooling", oldPoolConfig, is(nullValue()));

        ClientState<String, String> newState = clientStateRule.clientState.noIdleConnectionCleanup();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap copied.", clientStateRule.clientState.getBootstrap(), is(newState.getBootstrap()));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));
        assertThat("Pool config not copied", clientStateRule.clientState.getPoolConfig(),
                   is(not(newState.getPoolConfig())));

        final TestScheduler testScheduler = Schedulers.test();
        final TestSubscriber<Long> testSubscriber = new TestSubscriber<>();
        newState.getPoolConfig().getIdleConnectionsCleanupTimer()
                .observeOn(testScheduler)
                .subscribe(testSubscriber);
        testScheduler.advanceTimeBy(100, TimeUnit.DAYS); /*Basically never*/

        assertThat("Idle connection timer completed.", testSubscriber.getOnCompletedEvents(), is(empty()));
        assertThat("Idle connection timer errored.", testSubscriber.getOnErrorEvents(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testNoConnectionPooling() throws Exception {
        clientStateRule.clientState = clientStateRule.clientState.maxConnections(1);
        final PoolConfig<String, String> oldPoolConfig = clientStateRule.clientState.getPoolConfig();
        assertThat("Client state created with pooling", oldPoolConfig, is(notNullValue()));

        ClientState<String, String> newState = clientStateRule.clientState.noConnectionPooling();
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap copied.", clientStateRule.clientState.getBootstrap(), is(newState.getBootstrap()));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));
        assertThat("Unexpected pool config", newState.getPoolConfig(), is(nullValue()));
    }

    @Test(timeout = 60000)
    public void testRemoteAddress() throws Exception {
        final PoolConfig<String, String> oldPoolConfig = clientStateRule.clientState.getPoolConfig();
        assertThat("Client state created with pooling", oldPoolConfig, is(nullValue()));

        SocketAddress newAddress = new InetSocketAddress("localhost", 0);
        ClientState<String, String> newState = clientStateRule.clientState.remoteAddress(newAddress);
        assertThat("Client state not copied.", clientStateRule.clientState, is(not(newState)));
        assertThat("Bootstrap copied.", clientStateRule.clientState.getBootstrap(), is(newState.getBootstrap()));
        assertThat("Connection factory not copied.", clientStateRule.clientState.getConnectionFactory(),
                   is(not(newState.getConnectionFactory())));

        assertThat("remote address not set", newState.getRemoteAddress(), is(newAddress));
    }

    public static class ClientStateRule extends ExternalResource {

        private ClientState<String, String> clientState;
        private TcpServer<ByteBuf, ByteBuf> mockServer;
        private DetachedChannelPipeline mockPipeline;
        private TcpClientEventPublisher eventPublisher;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    mockPipeline = Mockito.mock(DetachedChannelPipeline.class, Mockito.RETURNS_MOCKS);

                    mockServer = TcpServer.newServer(0)
                                          .start(new ConnectionHandler<ByteBuf, ByteBuf>() {
                                              @Override
                                              public Observable<Void> handle(
                                                      Connection<ByteBuf, ByteBuf> newConnection) {
                                                  return newConnection.close();
                                              }
                                          });
                    eventPublisher = new TcpClientEventPublisher();
                    clientState = ClientState.create(mockPipeline, eventPublisher, new NioEventLoopGroup(),
                                                     NioSocketChannel.class,
                                                     new InetSocketAddress("localhost", mockServer.getServerPort()));
                    Mockito.verify(mockPipeline).getChannelInitializer();
                    base.evaluate();
                }
            };
        }

        public Channel connect() throws InterruptedException {
            return connect(clientState);
        }

        public Channel connect(final ClientState<?, ?> state) throws InterruptedException {
            state.getBootstrap().remoteAddress(state.getRemoteAddress());
            ChannelFuture connect = state.getBootstrap().connect();

            final AtomicReference<Channel> channelToReturn = new AtomicReference<>();
            connect.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        channelToReturn.set(future.channel());
                    }
                }
            });

            connect.sync().await(1, TimeUnit.MINUTES);

            final Channel toReturn = channelToReturn.get();

            assertThat("Connect failed, channel is null.", toReturn, is(notNullValue()));

            return toReturn;
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

        public void verifyMockPipelineAccessPostCopy(ClientState<String, String> newState) {
            Mockito.verify(mockPipeline).copy(Matchers.<Action1<ChannelPipeline>>anyObject());
            Mockito.verify(newState.getDetachedPipeline()).getChannelInitializer();
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