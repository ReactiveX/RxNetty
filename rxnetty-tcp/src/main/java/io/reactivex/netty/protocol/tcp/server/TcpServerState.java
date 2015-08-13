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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import io.reactivex.netty.protocol.tcp.ssl.DefaultSslCodec;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import io.reactivex.netty.server.ServerState;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;

/**
 * A collection of state that {@link TcpServer} holds.
 * This supports the copy-on-write semantics of {@link TcpServer}
 *
 * @param <R> The type of objects read from the server owning this state.
 * @param <W> The type of objects written to the server owning this state.
 */
public class TcpServerState<R, W> extends ServerState<R, W> {

    private final TcpServerEventPublisher eventPublisher;
    private final boolean secure;

    protected TcpServerState(SocketAddress socketAddress, EventLoopGroup parent,
                             EventLoopGroup child,
                             Class<? extends ServerChannel> channelClass) {
        super(socketAddress, parent, child, channelClass);
        secure = false;
        eventPublisher = new TcpServerEventPublisher();
    }

    protected TcpServerState(TcpServerState<?, ?> toCopy, SslCodec sslCodec) {
        super(toCopy, toCopy.detachedPipeline.configure(sslCodec));
        secure = true;
        eventPublisher = toCopy.eventPublisher.copy();
    }

    protected TcpServerState(TcpServerState<R, W> toCopy, SocketAddress socketAddress) {
        super(toCopy, socketAddress);
        secure = toCopy.secure;
        eventPublisher = toCopy.eventPublisher.copy();
    }

    protected TcpServerState(TcpServerState<R, W> toCopy, ServerBootstrap clone) {
        super(toCopy, clone);
        secure = toCopy.secure;
        eventPublisher = toCopy.eventPublisher.copy();
    }

    protected TcpServerState(TcpServerState<?, ?> toCopy, DetachedChannelPipeline newPipeline) {
        super(toCopy, newPipeline);
        secure = toCopy.secure;
        eventPublisher = toCopy.eventPublisher.copy();
    }

    public TcpServerState<R, W> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return secure(new DefaultSslCodec(sslEngineFactory));
    }

    public TcpServerState<R, W> secure(SSLEngine sslEngine) {
        return secure(new DefaultSslCodec(sslEngine));
    }

    public TcpServerState<R, W> secure(SslCodec sslCodec) {
        return new TcpServerState<>(this, sslCodec);
    }

    public TcpServerState<R, W> unsafeSecure() {
        return secure(new DefaultSslCodec(new Func1<ByteBufAllocator, SSLEngine>() {
            @Override
            public SSLEngine call(ByteBufAllocator allocator) {
                SelfSignedCertificate ssc;
                try {
                    ssc = new SelfSignedCertificate();
                    return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                            .build()
                                            .newEngine(allocator);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        }));
    }

    @Override
    protected ServerState<R, W> copyBootstrapOnly() {
        return new TcpServerState<>(this, bootstrap.clone());
    }

    @Override
    protected <RR, WW> ServerState<RR, WW> copy() {
        return new TcpServerState<>(this, detachedPipeline.copy());
    }

    @Override
    protected ServerState<R, W> copy(SocketAddress newSocketAddress) {
        return new TcpServerState<>(this, socketAddress);
    }

    public boolean isSecure() {
        return secure;
    }

    public TcpServerEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    /*package private. Should not leak as it is mutable*/ ServerBootstrap getBootstrap() {
        return bootstrap;
    }

    public static <RR, WW> TcpServerState<RR, WW> create(SocketAddress socketAddress) {
        return create(socketAddress, RxNetty.getRxEventLoopProvider().globalServerEventLoop(true),
                      RxNetty.isUsingNativeTransport() ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
    }

    public static <RR, WW> TcpServerState<RR, WW> create(SocketAddress socketAddress, EventLoopGroup group,
                                                      Class<? extends ServerChannel> channelClass) {
        return create(socketAddress, group, group, channelClass);
    }

    public static <RR, WW> TcpServerState<RR, WW> create(SocketAddress socketAddress, EventLoopGroup parent,
                                                      EventLoopGroup child,
                                                      Class<? extends ServerChannel> channelClass) {
        return new TcpServerState<>(socketAddress, parent, child, channelClass);
    }
}
