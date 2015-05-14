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
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ServerPool;
import io.reactivex.netty.client.ServerPool.Server;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

final class ConnectionRequestImpl<W, R> extends ConnectionRequest<W, R> {

    private final ClientState<W, R> clientState;

    ConnectionRequestImpl(final ClientState<W, R> clientState,
                          final ConcurrentMap<Server<ClientMetricsEvent<?>>, ClientState<W, R>> serverVsState) {
        super(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {

                if (!clientState.hasServerPool()) {
                    subscriber.onError(new IllegalArgumentException("Connection request created with no server pool but state map."));
                }

                ClientState<W, R> stateToUse;

                ServerPool<ClientMetricsEvent<?>> serverPool = clientState.getServerPool();
                try {
                    final Server<ClientMetricsEvent<?>> server = serverPool.next();
                    final ClientState<W, R> serverClientState = clientState.remoteAddress(server.getAddress());
                    ClientState<W, R> existing = serverVsState.putIfAbsent(server, serverClientState);
                    if (null == existing) {
                        stateToUse = serverClientState;
                        clientState.getEventsSubject().subscribe(server);
                        server.getLifecycle().subscribe(Actions.empty(), new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                serverVsState.remove(server);
                            }
                        }, new Action0() {
                            @Override
                            public void call() {
                                serverVsState.remove(server);
                            }
                        });
                    } else {
                        stateToUse = existing;
                    }
                } catch (NoSuchElementException e) {
                    subscriber.onError(e);
                    return;
                }

                stateToUse.getConnectionFactory()
                          .connect()
                          .unsafeSubscribe(subscriber);
            }
        });
        this.clientState = clientState;
    }

    ConnectionRequestImpl(final ClientState<W, R> clientState) {
        super(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                if (clientState.hasServerPool()) {
                    subscriber.onError(new IllegalArgumentException("Connection request created with a server pool but not provided a state map."));
                }

                clientState.getConnectionFactory()
                           .connect()
                           .unsafeSubscribe(subscriber);
            }
        });
        this.clientState = clientState;
    }

    @Override
    public ConnectionRequest<W, R> readTimeOut(final int timeOut, final TimeUnit timeUnit) {
        return addChannelHandlerFirst(HandlerNames.ClientReadTimeoutHandler.getName(), new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new InternalReadTimeoutHandler(timeOut, timeUnit);
            }
        });
    }

    @Override
    public ConnectionRequest<W, R> enableWireLogging(LogLevel wireLogginLevel) {
        return copy(clientState.enableWireLogging(wireLogginLevel));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerFirst(name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerFirst(group, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerLast(name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                    Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerLast(group, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                      Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerBefore(baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                      String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerBefore(group, baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerAfter(baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                     String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerAfter(group, baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return copy(clientState.<WW, RR>pipelineConfigurator(pipelineConfigurator));
    }

    @Override
    public ConnectionRequest<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return copy(clientState.secure(sslEngineFactory));
    }

    @Override
    public ConnectionRequest<W, R> secure(SSLEngine sslEngine) {
        return copy(clientState.secure(sslEngine));
    }

    @Override
    public ConnectionRequest<W, R> secure(SslCodec sslCodec) {
        return copy(clientState.secure(sslCodec));
    }

    @Override
    public ConnectionRequest<W, R> unsafeSecure() {
        return copy(clientState.unsafeSecure());
    }

    /*Visible for testing*/ClientState<W, R> getClientState() {
        return clientState;
    }

    private static <WW, RR> ConnectionRequestImpl<WW, RR> copy(ClientState<WW, RR> state) {
        return new ConnectionRequestImpl<>(state);
    }
}
