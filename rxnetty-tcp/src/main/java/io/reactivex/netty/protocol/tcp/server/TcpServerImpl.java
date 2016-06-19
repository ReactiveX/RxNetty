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
package io.reactivex.netty.protocol.tcp.server;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventListener;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import io.reactivex.netty.server.ServerState;
import io.reactivex.netty.ssl.SslCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TcpServerImpl<R, W> extends TcpServer<R, W> {

    private static final Logger logger = LoggerFactory.getLogger(TcpServerImpl.class);

    protected enum ServerStatus {Created, Starting, Started, Shutdown}

    private final ServerState<R, W> state;
    private ChannelFuture bindFuture;
    protected final AtomicReference<ServerStatus> serverStateRef;

    public TcpServerImpl(SocketAddress socketAddress) {
        state = TcpServerState.create(socketAddress);
        serverStateRef = new AtomicReference<>(ServerStatus.Created);
    }

    public TcpServerImpl(SocketAddress socketAddress, EventLoopGroup parent, EventLoopGroup child,
                         Class<? extends ServerChannel> channelClass) {
        state = TcpServerState.create(socketAddress, parent, child, channelClass);
        serverStateRef = new AtomicReference<>(ServerStatus.Created);
    }

    private TcpServerImpl(ServerState<R, W> state) {
        this.state = state;
        serverStateRef = new AtomicReference<>(ServerStatus.Created);
    }

    @Override
    public <T> TcpServer<R, W> channelOption(ChannelOption<T> option, T value) {
        return copy(state.channelOption(option, value));
    }

    @Override
    public <T> TcpServer<R, W> clientChannelOption(ChannelOption<T> option, T value) {
        return copy(state.clientChannelOption(option, value));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerFirst(name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerFirst(group, name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerLast(name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                            Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerLast(group, name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerBefore(String baseName, String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerBefore(baseName, name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerBefore(group, baseName, name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerAfter(String baseName, String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerAfter(baseName, name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<RR, WW>addChannelHandlerAfter(group, baseName, name, handlerFactory));
    }

    @Override
    public <RR, WW> TcpServer<RR, WW> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return copy(state.<RR, WW>pipelineConfigurator(pipelineConfigurator));
    }

    @Override
    public TcpServer<R, W> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return copy(((TcpServerState<R, W>)state).secure(sslEngineFactory));
    }

    @Override
    public TcpServer<R, W> secure(SSLEngine sslEngine) {
        return copy(((TcpServerState<R, W>)state).secure(sslEngine));
    }

    @Override
    public TcpServer<R, W> secure(SslCodec sslCodec) {
        return copy(((TcpServerState<R, W>)state).secure(sslCodec));
    }

    @Override
    public TcpServer<R, W> unsafeSecure() {
        return copy(((TcpServerState<R, W>)state).unsafeSecure());
    }

    @Override
    @Deprecated
    public TcpServer<R, W> enableWireLogging(LogLevel wireLoggingLevel) {
        return copy(state.<W, R>enableWireLogging(wireLoggingLevel));
    }

    @Override
    public TcpServer<R, W> enableWireLogging(String name, LogLevel wireLoggingLevel) {
        return copy(state.<W, R>enableWireLogging(name, wireLoggingLevel));
    }

    @Override
    public int getServerPort() {

        final SocketAddress localAddress = getServerAddress();
        if (localAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) localAddress).getPort();
        } else {
            return 0;
        }
    }

    @Override
    public SocketAddress getServerAddress() {
        SocketAddress localAddress;
        if (null != bindFuture && bindFuture.isDone()) {
            localAddress = bindFuture.channel().localAddress();
        } else {
            localAddress = state.getServerAddress();
        }
        return localAddress;
    }

    @Override
    public TcpServer<R, W> start(final ConnectionHandler<R, W> connectionHandler) {
        if (!serverStateRef.compareAndSet(ServerStatus.Created, ServerStatus.Starting)) {
            throw new IllegalStateException("Server already started");
        }
        try {
            Action1<ChannelPipeline> handlerFactory = new Action1<ChannelPipeline>() {
                @Override
                public void call(ChannelPipeline pipeline) {
                    TcpServerState<R, W> tcpState = (TcpServerState<R, W>) state;
                    TcpServerConnectionToChannelBridge.addToPipeline(pipeline, connectionHandler,
                                                                     tcpState.getEventPublisher(), tcpState.isSecure());
                }
            };
            final TcpServerState<R, W> newState = (TcpServerState<R, W>) state.pipelineConfigurator(handlerFactory);
            bindFuture = newState.getBootstrap().bind(newState.getServerAddress()).sync();
            if (!bindFuture.isSuccess()) {
                throw new RuntimeException(bindFuture.cause());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        serverStateRef.set(ServerStatus.Started); // It will come here only if this was the thread that transitioned to Starting

        logger.info("Rx server started at port: " + getServerPort());

        return this;
    }

    @Override
    public void shutdown() {
        if (!serverStateRef.compareAndSet(ServerStatus.Started, ServerStatus.Shutdown)) {
            throw new IllegalStateException("The server is already shutdown.");
        } else {
            try {
                bindFuture.channel().close().sync();
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for the server socket to close.", e);
            }
        }
    }

    @Override
    public void awaitShutdown() {
        ServerStatus status = serverStateRef.get();
        switch (status) {
        case Created:
        case Starting:
            throw new IllegalStateException("Server not started yet.");
        case Started:
            try {
                bindFuture.channel().closeFuture().await();
            } catch (InterruptedException e) {
                Thread.interrupted(); // Reset the interrupted status
                logger.error("Interrupted while waiting for the server socket to close.", e);
            }
            break;
        case Shutdown:
            // Nothing to do as it is already shutdown.
            break;
        }
    }

    @Override
    public void awaitShutdown(long duration, TimeUnit timeUnit) {
        ServerStatus status = serverStateRef.get();
        switch (status) {
        case Created:
        case Starting:
            throw new IllegalStateException("Server not started yet.");
        case Started:
            try {
                bindFuture.channel().closeFuture().await(duration, timeUnit);
            } catch (InterruptedException e) {
                Thread.interrupted(); // Reset the interrupted status
                logger.error("Interrupted while waiting for the server socket to close.", e);
            }
            break;
        case Shutdown:
            // Nothing to do as it is already shutdown.
            break;
        }
    }

    @Override
    public TcpServerEventPublisher getEventPublisher() {
        return ((TcpServerState<R, W>)state).getEventPublisher();
    }

    @Override
    public Subscription subscribe(TcpServerEventListener listener) {
        return ((TcpServerState<R, W>)state).getEventPublisher().subscribe(listener);
    }

    private static <RR, WW> TcpServer<RR, WW> copy(ServerState<RR, WW> newState) {
        return new TcpServerImpl<>(newState);
    }
}
