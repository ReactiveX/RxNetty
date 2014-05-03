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

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractServerBuilder<I, O, T extends AbstractBootstrap<T, C>, C extends Channel,
        B extends AbstractServerBuilder, S extends AbstractServer<I, O, T, C, S>> {

    protected final T serverBootstrap;
    protected PipelineConfigurator<I, O> pipelineConfigurator;
    protected Class<? extends C> serverChannelClass;
    protected final ConnectionHandler<I, O> connectionHandler;
    protected final int port;

    protected AbstractServerBuilder(int port, T bootstrap, ConnectionHandler<I, O> connectionHandler) {
        if (null == connectionHandler) {
            throw new IllegalArgumentException("Connection handler can not be null");
        }
        if (null == bootstrap) {
            throw new IllegalArgumentException("Server bootstrap can not be null");
        }
        this.port = port;
        serverBootstrap = bootstrap;
        this.connectionHandler = connectionHandler;
        defaultChannelOptions();
    }

    public B eventLoop(EventLoopGroup singleGroup) {
        serverBootstrap.group(singleGroup);
        return returnBuilder();
    }

    public B channel(Class<C> serverChannelClass) {
        this.serverChannelClass = serverChannelClass;
        return returnBuilder();
    }

    public <T> B channelOption(ChannelOption<T> option, T value) {
        serverBootstrap.option(option, value);
        return returnBuilder();
    }

    public B pipelineConfigurator(PipelineConfigurator<I, O> pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return returnBuilder();
    }

    public B defaultChannelOptions() {
        channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return returnBuilder();
    }

    public S build() {
        if (null == serverChannelClass) {
            serverChannelClass = defaultServerChannelClass();
            EventLoopGroup acceptorGroup = serverBootstrap.group();
            if (null == acceptorGroup) {
                serverBootstrap.group(RxNetty.getRxEventLoopProvider().globalServerEventLoop());
            }
        }

        if (null == serverBootstrap.group()) {
            if (defaultServerChannelClass() == serverChannelClass) {
                serverBootstrap.group(RxNetty.getRxEventLoopProvider().globalServerEventLoop());
            } else {
                // Fail fast for defaults we do not support.
                throw new IllegalStateException("Specified a channel class but not the event loop group.");
            }
        }

        serverBootstrap.channel(serverChannelClass);
        return createServer();
    }

    protected abstract Class<? extends C> defaultServerChannelClass();

    protected abstract S createServer();

    @SuppressWarnings("unchecked")
    protected B returnBuilder() {
        return (B) this;
    }
}
