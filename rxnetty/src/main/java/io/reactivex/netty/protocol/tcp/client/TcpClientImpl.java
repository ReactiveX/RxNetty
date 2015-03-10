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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.codec.SSLCodec;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TcpClientImpl<W, R> extends TcpClient<W, R> {

    private final ClientState<W, R> state;
    private final String name;
    private final ConcurrentMap<SocketAddress, ConnectionRequest<W, R>> remoteAddrVsConnRequest;
    private final ConnectionRequestImpl<W, R> thisConnectionRequest;

    public TcpClientImpl(String name, SocketAddress remoteAddress) {
        this(name, RxNetty.getRxEventLoopProvider().globalClientEventLoop(), NioSocketChannel.class, remoteAddress);
    }

    public TcpClientImpl(String name, EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass,
                         SocketAddress remoteAddress) {
        this.name = name;
        state = ClientState.create(eventLoopGroup, channelClass, remoteAddress);
        remoteAddrVsConnRequest = new ConcurrentHashMap<>();
        thisConnectionRequest = new ConnectionRequestImpl<>(state);
    }

    public TcpClientImpl(String name, ClientState<W, R> state) {
        this.name = name;
        this.state = state;
        remoteAddrVsConnRequest = new ConcurrentHashMap<>();
        thisConnectionRequest = new ConnectionRequestImpl<>(state);
    }

    protected TcpClientImpl(TcpClientImpl<?, ?> client, ClientState<W, R> state) {
        this.state = state;
        name = client.name;
        remoteAddrVsConnRequest = new ConcurrentHashMap<>(); // Since, the state has changed, no existing requests are valid.
        thisConnectionRequest = new ConnectionRequestImpl<>(this.state);
    }

    @Override
    public ConnectionRequest<W, R> createConnectionRequest() {
        return thisConnectionRequest;
    }

    @Override
    public ConnectionRequest<W, R> createConnectionRequest(String host, int port) {
        return createConnectionRequest(new InetSocketAddress(host, port));
    }

    @Override
    public ConnectionRequest<W, R> createConnectionRequest(InetAddress host, int port) {
        return createConnectionRequest(new InetSocketAddress(host, port));
    }

    @Override
    public ConnectionRequest<W, R> createConnectionRequest(SocketAddress remoteAddress) {
        final ConnectionRequest<W, R> connectionRequest = remoteAddrVsConnRequest.get(remoteAddress);

        if (null != connectionRequest) {
            return connectionRequest;
        }

        ConnectionRequestImpl<W, R> newRequest = new ConnectionRequestImpl<>(state.remoteAddress(remoteAddress));

        ConnectionRequest<W, R> existingReq = remoteAddrVsConnRequest.putIfAbsent(remoteAddress, newRequest);

        return null != existingReq ? existingReq : newRequest;
    }

    @Override
    public <T> TcpClient<W, R> channelOption(ChannelOption<T> option, T value) {
        return copy(state.channelOption(option, value));
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
        return new TcpClientImpl<WW, RR>(this, state.<WW, RR>addChannelHandlerLast(group, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(String baseName, String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerBefore(baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerBefore(group, baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerAfter(String baseName, String name, Func0<ChannelHandler> handlerFactory) {
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
    public TcpClient<W, R> maxConnections(int maxConnections) {
        return copy(state.maxConnections(maxConnections));
    }

    @Override
    public TcpClient<W, R> idleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis) {
        return copy(state.maxIdleTimeoutMillis(idleConnectionsTimeoutMillis));
    }

    @Override
    public TcpClient<W, R> connectionPoolLimitStrategy(PoolLimitDeterminationStrategy limitDeterminationStrategy) {
        return copy(state.connectionPoolLimitStrategy(limitDeterminationStrategy));
    }

    @Override
    public TcpClient<W, R> idleConnectionCleanupTimer(Observable<Long> idleConnectionCleanupTimer) {
        return copy(state.idleConnectionCleanupTimer(idleConnectionCleanupTimer));
    }

    @Override
    public TcpClient<W, R> noIdleConnectionCleanup() {
        return copy(state.noIdleConnectionCleanup());
    }

    @Override
    public TcpClient<W, R> noConnectionPooling() {
        return copy(state.noConnectionPooling());
    }

    @Override
    public TcpClient<W, R> enableWireLogging(LogLevel wireLoggingLevel) {
        return copy(state.enableWireLogging(wireLoggingLevel));
    }

    @Override
    public TcpClient<W, R> sslEngineFactory(SSLEngineFactory sslEngineFactory) {
        return copy(state.<W, R>pipelineConfigurator(new SSLCodec(sslEngineFactory)));
    }

    private <WW, RR> TcpClientImpl<WW, RR> copy(ClientState<WW, RR> state) {
        return new TcpClientImpl<WW, RR>(this, state);
    }

    @Override
    public Subscription subscribe(MetricEventsListener<? extends ClientMetricsEvent<?>> listener) {
        return state.getEventsSubject().subscribe(listener);
    }
}
