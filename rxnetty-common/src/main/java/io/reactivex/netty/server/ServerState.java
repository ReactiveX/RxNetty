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
package io.reactivex.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.HandlerNames;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.channel.WriteTransformer;
import rx.functions.Action1;
import rx.functions.Func0;

import java.net.SocketAddress;

/**
 * A collection of state that a server holds.
 *
 * @param <R> The type of objects read from the server owning this state.
 * @param <W> The type of objects written to the server owning this state.
 */
public abstract class ServerState<R, W> {

    protected final SocketAddress socketAddress;
    protected final ServerBootstrap bootstrap;
    protected final DetachedChannelPipeline detachedPipeline;

    protected ServerState(SocketAddress socketAddress, EventLoopGroup parent, EventLoopGroup child,
                          Class<? extends ServerChannel> channelClass) {
        this.socketAddress = socketAddress;
        bootstrap = new ServerBootstrap();
        bootstrap.childOption(ChannelOption.AUTO_READ, false); // by default do not read content unless asked.
        bootstrap.group(parent, child);
        bootstrap.channel(channelClass);
        detachedPipeline = new DetachedChannelPipeline();
        detachedPipeline.addLast(HandlerNames.WriteTransformer.getName(), new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new WriteTransformer();
            }
        });
        bootstrap.childHandler(detachedPipeline.getChannelInitializer());
    }

    protected ServerState(ServerState<R, W> toCopy, final ServerBootstrap newBootstrap) {
        socketAddress = toCopy.socketAddress;
        bootstrap = newBootstrap;
        detachedPipeline = toCopy.detachedPipeline;
        bootstrap.childHandler(detachedPipeline.getChannelInitializer());
    }

    protected ServerState(ServerState<?, ?> toCopy, final DetachedChannelPipeline newPipeline) {
        final ServerState<R, W> toCopyCast = toCopy.cast();
        socketAddress = toCopy.socketAddress;
        bootstrap = toCopyCast.bootstrap.clone();
        detachedPipeline = newPipeline;
        bootstrap.childHandler(detachedPipeline.getChannelInitializer());
    }

    protected ServerState(ServerState<R, W> toCopy, final SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
        bootstrap = toCopy.bootstrap.clone();
        detachedPipeline = toCopy.detachedPipeline;
        bootstrap.childHandler(detachedPipeline.getChannelInitializer());
    }

    public <T> ServerState<R, W> channelOption(ChannelOption<T> option, T value) {
        ServerState<R, W> copy = copyBootstrapOnly();
        copy.bootstrap.option(option, value);
        return copy;
    }

    public <T> ServerState<R, W> clientChannelOption(ChannelOption<T> option, T value) {
        ServerState<R, W> copy = copyBootstrapOnly();
        copy.bootstrap.childOption(option, value);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addFirst(name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addFirst(group, name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addLast(name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addLast(group, name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerBefore(String baseName, String name,
                                                                Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addBefore(baseName, name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                String name, Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addBefore(group, baseName, name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerAfter(String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addAfter(baseName, name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                               String name, Func0<ChannelHandler> handlerFactory) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.addAfter(group, baseName, name, handlerFactory);
        return copy;
    }

    public <RR, WW> ServerState<RR, WW> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        ServerState<RR, WW> copy = copy();
        copy.detachedPipeline.configure(pipelineConfigurator);
        return copy;
    }

    public ServerState<R, W> enableWireLogging(final LogLevel wireLoggingLevel) {
        return enableWireLogging(LoggingHandler.class.getName(), wireLoggingLevel);
    }

    public ServerState<R, W> enableWireLogging(final String name, final LogLevel wireLoggingLevel) {
        return addChannelHandlerFirst(HandlerNames.WireLogging.getName(), new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new LoggingHandler(name, wireLoggingLevel);
            }
        });
    }

    public ServerState<R, W> serverAddress(SocketAddress socketAddress) {
        return copy(socketAddress);
    }

    public SocketAddress getServerAddress() {
        return socketAddress;
    }

    /*package private. Should not leak as it is mutable*/ ServerBootstrap getBootstrap() {
        return bootstrap;
    }

    protected abstract ServerState<R, W> copyBootstrapOnly();

    protected abstract <RR, WW> ServerState<RR, WW> copy();

    protected abstract ServerState<R, W> copy(SocketAddress newSocketAddress);

    @SuppressWarnings("unchecked")
    private <RR, WW> ServerState<RR, WW> cast() {
        return (ServerState<RR, WW>) this;
    }
}
