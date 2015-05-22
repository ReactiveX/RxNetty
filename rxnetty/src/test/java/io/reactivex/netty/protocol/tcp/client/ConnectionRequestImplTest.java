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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.functions.Action1;
import rx.functions.Func0;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionRequestImplTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private ClientState<String, String> state;

    @Test(timeout = 60000)
    public void testReadTimeOut() throws Exception {

    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirst() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>) req.<String, String>addChannelHandlerFirst("handler", factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerFirst("handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirstWithExecutor() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>) req.<String, String>addChannelHandlerFirst(group, "handler",
                                                                                                   factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerFirst(group, "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLast() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>) req.<String, String>addChannelHandlerLast("handler", factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerLast("handler", factory);

    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLastWithExecutor() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>)req.<String, String>addChannelHandlerLast(group, "handler",
                                                                                                 factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerLast(group, "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerBefore() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>) req.<String, String>addChannelHandlerBefore("base", "handler",
                                                                                               factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerBefore("base", "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerBeforeWithExecutor() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>)req.<String, String>addChannelHandlerBefore(group, "base",
                                                                                                   "handler", factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerBefore(group, "base", "handler", factory);

    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerAfter() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>) req.<String, String>addChannelHandlerAfter("base", "handler",
                                                                                                   factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerAfter("base", "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerAfterWithExecutor() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<>(state);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>) req.<String, String>addChannelHandlerAfter(group, "base",
                                                                                                   "handler", factory);

        assertDeepRequestCopy(req, newReq);

        verify(state).addChannelHandlerAfter(group, "base", "handler", factory);

    }

    @Test(timeout = 60000)
    public void testPipelineConfigurator() throws Exception {
        ConnectionRequestImpl<String, String> req = new ConnectionRequestImpl<String, String>(state);
        Action1<ChannelPipeline> configurator = new Action1<ChannelPipeline>() {
            @Override
            public void call(ChannelPipeline pipeline) {
            }
        };
        ConnectionRequestImpl<String, String> newReq =
                (ConnectionRequestImpl<String, String>) req.<String, String>pipelineConfigurator(configurator);

        assertDeepRequestCopy(req, newReq);

        verify(state).pipelineConfigurator(configurator);
    }

    private static void assertDeepRequestCopy(ConnectionRequestImpl<String, String> req,
                                              ConnectionRequestImpl<String, String> newReq) {
        assertThat("Client was not copied.", newReq, is(not(req)));
        assertThat("Client state was not copied.", newReq.getClientState(),
                   is(not(req.getClientState())));
    }

    private static Func0<ChannelHandler> newHandlerFactory() {
        return new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };
    }
}