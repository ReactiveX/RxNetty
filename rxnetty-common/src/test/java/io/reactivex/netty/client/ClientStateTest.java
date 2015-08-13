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
package io.reactivex.netty.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.HandlerNames;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.test.util.MockEventPublisherFactory;
import io.reactivex.netty.util.LoggingHandlerFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Matchers;
import org.mockito.Mockito;
import rx.functions.Action1;
import rx.functions.Func0;

import java.net.InetSocketAddress;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ClientStateTest {

    @Rule
    public final ClientStateRule clientStateRule = new ClientStateRule();

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
        private DetachedChannelPipeline mockPipeline;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    mockPipeline = Mockito.mock(DetachedChannelPipeline.class, Mockito.RETURNS_MOCKS);
                    clientState = ClientState.create(mockPipeline, new MockEventPublisherFactory(),
                                                     ConnectionProvider.<String, String>forHost(new InetSocketAddress(0)))
                                             .enableWireLogging(LogLevel.ERROR);
                    base.evaluate();
                }
            };
        }

        public Func0<ChannelHandler> newHandler() {
            return new Func0<ChannelHandler>() {
                @Override
                public ChannelHandler call() {
                    return new TestableChannelHandler();
                }
            };
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