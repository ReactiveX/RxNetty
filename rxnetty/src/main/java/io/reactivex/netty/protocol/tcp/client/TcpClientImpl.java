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

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.internal.EventPublisherFactory;
import io.reactivex.netty.protocol.tcp.client.internal.TcpEventPublisherFactory;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.util.concurrent.TimeUnit;

public final class TcpClientImpl<W, R> extends TcpClient<W, R> {

    private final ClientState<W, R> state;
    private final ConnectionRequestImpl<W, R> thisConnectionRequest;

    /*Visible for testing*/ TcpClientImpl(ClientState<W, R> state) {
        this.state = state;
        thisConnectionRequest = new ConnectionRequestImpl<>(state);
    }

    @Override
    public ConnectionRequest<W, R> createConnectionRequest() {
        return thisConnectionRequest;
    }

    @Override
    public <T> TcpClient<W, R> channelOption(ChannelOption<T> option, T value) {
        return copy(state.channelOption(option, value));
    }

    @Override
    public TcpClient<W, R> readTimeOut(final int timeOut, final TimeUnit timeUnit) {
        return addChannelHandlerFirst(HandlerNames.ClientReadTimeoutHandler.getName(), new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new InternalReadTimeoutHandler(timeOut, timeUnit);
            }
        });
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerFirst(name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerFirst(group, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerLast(name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                            Func0<ChannelHandler> handlerFactory) {
        return new TcpClientImpl<WW, RR>(state.<WW, RR>addChannelHandlerLast(group, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerBefore(baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerBefore(group, baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerAfter(baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerAfter(group, baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return copy(state.<WW, RR>pipelineConfigurator(pipelineConfigurator));
    }

    @Override
    public TcpClient<W, R> enableWireLogging(LogLevel wireLoggingLevel) {
        return copy(state.enableWireLogging(wireLoggingLevel));
    }

    @Override
    public TcpClient<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return copy(state.secure(sslEngineFactory));
    }

    @Override
    public TcpClient<W, R> secure(SSLEngine sslEngine) {
        return copy(state.secure(sslEngine));
    }

    @Override
    public TcpClient<W, R> secure(SslCodec sslCodec) {
        return copy(state.secure(sslCodec));
    }

    @Override
    public TcpClient<W, R> unsafeSecure() {
        return copy(state.unsafeSecure());
    }

    public static <W, R> TcpClientImpl<W, R> create(ConnectionProvider<W, R> connectionProvider) {
        return create(connectionProvider, new TcpEventPublisherFactory());
    }

    public static <W, R> TcpClientImpl<W, R> create(ConnectionProvider<W, R> connectionProvider,
                                                EventPublisherFactory eventPublisherFactory) {
        return new TcpClientImpl<>(ClientState.create(connectionProvider, eventPublisherFactory));
    }

    private static <WW, RR> TcpClientImpl<WW, RR> copy(ClientState<WW, RR> state) {
        return new TcpClientImpl<WW, RR>(state);
    }

    /*Visible for testing*/ ClientState<W, R> getClientState() {
        return state;
    }

    @Override
    public Subscription subscribe(TcpClientEventListener listener) {
        return state.getEventPublisherFactory().subscribe(listener);
    }
}
