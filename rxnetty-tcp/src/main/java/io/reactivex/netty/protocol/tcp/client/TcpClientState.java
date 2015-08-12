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

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.client.ClientState;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.internal.EventPublisherFactory;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.ssl.DefaultSslCodec;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;

/**
 * A collection of state that a {@link TcpClient} holds. This supports the copy-on-write semantics of {@link TcpClient}.
 *
 * @param <W> The type of objects written to the client owning this state.
 * @param <R> The type of objects read from the client owning this state.
 */
public class TcpClientState<W, R> extends ClientState<W, R> {

    protected TcpClientState(EventPublisherFactory<TcpClientEventListener> eventPublisherFactory,
                             DetachedChannelPipeline detachedPipeline,
                             ConnectionProvider<W, R> rawConnectionProvider) {
        super(eventPublisherFactory, detachedPipeline, rawConnectionProvider);
    }

    protected TcpClientState(TcpClientState<W, R> toCopy, SslCodec sslCodec) {
        super(toCopy, toCopy.detachedPipeline.copy(new TailHandlerFactory(true)).configure(sslCodec), true);
    }

    protected TcpClientState(TcpClientState<?, ?> toCopy, DetachedChannelPipeline newPipeline, boolean isSecure) {
        super(toCopy, newPipeline, isSecure);
    }

    public TcpClientState<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return secure(new DefaultSslCodec(sslEngineFactory));
    }

    public TcpClientState<W, R> secure(SSLEngine sslEngine) {
        return secure(new DefaultSslCodec(sslEngine));
    }

    public TcpClientState<W, R> secure(SslCodec sslCodec) {
        TcpClientState<W, R> toReturn = new TcpClientState<>(this, sslCodec);
        toReturn.realizeState();
        return toReturn;
    }

    public TcpClientState<W, R> unsafeSecure() {
        return secure(new DefaultSslCodec(new Func1<ByteBufAllocator, SSLEngine>() {
            @Override
            public SSLEngine call(ByteBufAllocator allocator) {
                try {
                    return SslContextBuilder.forClient()
                                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                            .build()
                                            .newEngine(allocator);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        }));
    }

    @Override
    protected <WW, RR> ClientState<WW, RR> copyStateInstance() {
        TailHandlerFactory newTail = new TailHandlerFactory(isSecure);
        return new TcpClientState<>(this, detachedPipeline.copy(newTail), isSecure);
    }

    public static <WW, RR> TcpClientState<WW, RR> createTcpState(ConnectionProvider<WW, RR> connectionProvider,
                                                                 EventPublisherFactory<TcpClientEventListener> epf) {
        DetachedChannelPipeline pipeline = newChannelPipeline(new TailHandlerFactory(false));
        TcpClientState<WW, RR> toReturn = new TcpClientState<>(epf, pipeline, connectionProvider);
        toReturn.realizeState();
        return toReturn;
    }
}
