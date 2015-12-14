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
package io.reactivex.netty.protocol.tcp.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionRequest;
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
    private TcpClient<String, String> client;
    @Mock()
    private ConnectionProvider<String, String> connectionProvider;

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirst() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerFirst("handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerFirst("handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerFirstWithExecutor() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerFirst(group, "handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerFirst(group, "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLast() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerLast("handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerLast("handler", factory);

    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerLastWithExecutor() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerLast(group, "handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerLast(group, "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerBefore() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerBefore("base", "handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerBefore("base", "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerBeforeWithExecutor() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerBefore(group, "base", "handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerBefore(group, "base", "handler", factory);

    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerAfter() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerAfter("base", "handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerAfter("base", "handler", factory);
    }

    @Test(timeout = 60000)
    public void testAddChannelHandlerAfterWithExecutor() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Func0<ChannelHandler> factory = newHandlerFactory();
        EventExecutorGroup group = new NioEventLoopGroup();
        ConnectionRequest<String, String> newReq = req.addChannelHandlerAfter(group, "base", "handler", factory);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).addChannelHandlerAfter(group, "base", "handler", factory);

    }

    @Test(timeout = 60000)
    public void testPipelineConfigurator() throws Exception {
        ConnectionRequest<String, String> req = new ConnectionRequestImpl<>(connectionProvider, client);
        Action1<ChannelPipeline> configurator = new Action1<ChannelPipeline>() {
            @Override
            public void call(ChannelPipeline pipeline) {
            }
        };
        ConnectionRequest<String, String> newReq = req.pipelineConfigurator(configurator);

        assertThat("Request was not copied.", newReq, is(not(req)));

        verify(client).pipelineConfigurator(configurator);
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