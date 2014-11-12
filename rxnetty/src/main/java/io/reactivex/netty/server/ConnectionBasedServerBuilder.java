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

package io.reactivex.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class ConnectionBasedServerBuilder<I, O, B extends ConnectionBasedServerBuilder>
        extends AbstractServerBuilder<I,O, ServerBootstrap, ServerChannel, B, RxServer<I, O>> {

    protected ConnectionBasedServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        this(port, connectionHandler, new ServerBootstrap());
    }

    protected ConnectionBasedServerBuilder(int port, ConnectionHandler<I, O> connectionHandler,
                                           ServerBootstrap bootstrap) {
        super(port, bootstrap, connectionHandler);
    }

    public B eventLoops(EventLoopGroup acceptorGroup, EventLoopGroup workerGroup) {
        serverBootstrap.group(acceptorGroup, workerGroup);
        return returnBuilder();
    }

    public <T> B childChannelOption(ChannelOption<T> option, T value) {
        serverBootstrap.childOption(option, value);
        return returnBuilder();
    }

    @Override
    protected Class<? extends ServerChannel> defaultServerChannelClass() {
        if (RxNetty.isUsingNativeTransport()) {
            return EpollServerSocketChannel.class;
        }
        return NioServerSocketChannel.class;
    }

    @Override
    protected void configureDefaultEventloopGroup() {
        serverBootstrap.group(RxNetty.getRxEventLoopProvider().globalServerParentEventLoop(true),
                              RxNetty.getRxEventLoopProvider().globalServerEventLoop(true));
    }

    @Override
    public B defaultChannelOptions() {
        channelOption(ChannelOption.SO_KEEPALIVE, true);
        childChannelOption(ChannelOption.SO_KEEPALIVE, true);
        childChannelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return super.defaultChannelOptions();
    }
}
