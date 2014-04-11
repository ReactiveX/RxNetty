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
package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.pool.ChannelPool;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractClientBuilder<I, O, B extends AbstractClientBuilder, C extends RxClient<I, O>> {

    protected final RxClientImpl.ServerInfo serverInfo;
    protected final Bootstrap bootstrap;
    protected PipelineConfigurator<O, I> pipelineConfigurator;
    protected Class<? extends SocketChannel> socketChannel;
    protected EventLoopGroup eventLoopGroup;
    protected RxClient.ClientConfig clientConfig;
    protected ChannelPool channelPool;

    protected AbstractClientBuilder(Bootstrap bootstrap, String host, int port) {
        this.bootstrap = bootstrap;
        serverInfo = new RxClientImpl.ServerInfo(host, port);
        clientConfig = RxClient.ClientConfig.DEFAULT_CONFIG;
        defaultChannelOptions();
    }

    protected AbstractClientBuilder(String host, int port) {
        this(new Bootstrap(), host, port);
    }

    public B defaultChannelOptions() {
        channelOption(ChannelOption.SO_KEEPALIVE, true);
        channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return channelOption(ChannelOption.TCP_NODELAY, true);
    }

    public B pipelineConfigurator(PipelineConfigurator<O, I> pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return returnBuilder();
    }

    public <T> B channelOption(ChannelOption<T> option, T value) {
        bootstrap.option(option, value);
        return returnBuilder();
    }

    public B channel(Class<? extends SocketChannel> socketChannel) {
        this.socketChannel = socketChannel;
        return returnBuilder();
    }

    public B eventloop(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return returnBuilder();
    }

    public B config(RxClient.ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        return returnBuilder();
    }

    public B channelPool(ChannelPool pool) {
        this.channelPool = pool;
        return returnBuilder();
    }
    
    public C build() {
        if (null == socketChannel) {
            socketChannel = NioSocketChannel.class;
            if (null == eventLoopGroup) {
                eventLoopGroup = RxNetty.getRxEventLoopProvider().globalClientEventLoop();
            }
        }

        if (null == eventLoopGroup) {
            if (NioSocketChannel.class == socketChannel) {
                eventLoopGroup = RxNetty.getRxEventLoopProvider().globalClientEventLoop();
            } else {
                // Fail fast for defaults we do not support.
                throw new IllegalStateException("Specified a channel class but not the event loop group.");
            }
        }

        bootstrap.channel(socketChannel).group(eventLoopGroup);
        return createClient();
    }

    protected abstract C createClient();

    @SuppressWarnings("unchecked")
    protected B returnBuilder() {
        return (B) this;
    }
}
