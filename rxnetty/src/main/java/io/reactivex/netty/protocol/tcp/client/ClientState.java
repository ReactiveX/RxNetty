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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ClientConnectionToChannelBridge;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.client.PoolConfig;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.client.PreferCurrentEventLoopGroup;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.tcp.client.PreferCurrentEventLoopHolder.IdleConnectionsHolderFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.client.PoolConfig.*;

/**
 * A collection of state that {@link TcpClient} holds. This supports the copy-on-write semantics of {@link TcpClient}
 *
 * @param <W> The type of objects written to the client owning this state.
 * @param <R> The type of objects read from the client owning this state.
 *
 * @author Nitesh Kant
 */
public final class ClientState<W, R> {

    private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;

    private ClientConnectionFactory<W, R> connectionFactory; // Not final since it depends on ClientState
    private final Bootstrap clientBootstrap;
    private final PoolConfig<W, R> poolConfig;
    private final DetachedChannelPipeline detachedPipeline;
    private final SocketAddress remoteAddress;

    private ClientState(EventLoopGroup group, Class<? extends Channel> channelClass, SocketAddress remoteAddress) {
        clientBootstrap = new Bootstrap();
        clientBootstrap.option(ChannelOption.AUTO_READ, false); // by default do not read content unless asked.
        clientBootstrap.group(group);
        clientBootstrap.channel(channelClass);
        poolConfig = null;
        eventsSubject = new MetricEventsSubject<ClientMetricsEvent<?>>();
        detachedPipeline = new DetachedChannelPipeline(new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ClientConnectionToChannelBridge<W, R>(eventsSubject);
            }
        });
        clientBootstrap.handler(detachedPipeline.getChannelInitializer());
        this.remoteAddress = remoteAddress;
    }

    private ClientState(ClientState<W, R> toCopy, Bootstrap newBootstrap) {
        clientBootstrap = newBootstrap;
        poolConfig = null == toCopy.poolConfig ? null : toCopy.poolConfig.copy();
        detachedPipeline = toCopy.detachedPipeline;
        clientBootstrap.handler(detachedPipeline.getChannelInitializer());
        eventsSubject = toCopy.eventsSubject.copy();
        remoteAddress = toCopy.remoteAddress;
    }

    private ClientState(ClientState<W, R> toCopy, PoolConfig<W, R> poolConfig) {
        clientBootstrap = toCopy.clientBootstrap;
        this.poolConfig = poolConfig;
        detachedPipeline = toCopy.detachedPipeline;
        clientBootstrap.handler(detachedPipeline.getChannelInitializer());
        eventsSubject = toCopy.eventsSubject.copy();
        remoteAddress = toCopy.remoteAddress;
    }

    private ClientState(ClientState<?, ?> toCopy, DetachedChannelPipeline newPipeline) {
        final ClientState<W, R> toCopyCast = toCopy.cast();
        clientBootstrap = toCopyCast.clientBootstrap.clone();
        poolConfig = null == toCopyCast.poolConfig ? null : toCopyCast.poolConfig.copy();
        detachedPipeline = newPipeline;
        clientBootstrap.handler(detachedPipeline.getChannelInitializer());
        eventsSubject = toCopyCast.eventsSubject.copy();
        remoteAddress = toCopy.remoteAddress;
    }

    private ClientState(ClientState<?, ?> toCopy, SocketAddress newAddress) {
        final ClientState<W, R> toCopyCast = toCopy.cast();
        clientBootstrap = toCopyCast.clientBootstrap;
        poolConfig = toCopyCast.poolConfig;
        detachedPipeline = toCopy.detachedPipeline;
        eventsSubject = toCopyCast.eventsSubject.copy();
        remoteAddress = newAddress;
    }

    public <T> ClientState<W, R> channelOption(ChannelOption<T> option, T value) {
        ClientState<W, R> copy = copyBootstrapOnly();
        copy.clientBootstrap.option(option, value);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addFirst(name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addFirst(group, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addLast(name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addLast(group, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addBefore(baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addBefore(group, baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addAfter(baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                               String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addAfter(group, baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        ClientState<WW, RR> copy = copy();
        copy.pipelineConfigurator(pipelineConfigurator);
        return copy;
    }

    public ClientState<W, R> enableWireLogging(final LogLevel wireLogginLevel) {
        return addChannelHandlerFirst(HandlerNames.WireLogging.getName(), new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new LoggingHandler(wireLogginLevel);
            }
        });
    }

    public ClientState<W, R> maxConnections(int maxConnections) {
        PoolConfig<W, R> newPoolConfig;
        if (null != poolConfig) {
            newPoolConfig = poolConfig.maxConnections(maxConnections);
        } else {
            long maxIdleTimeMillis = DEFAULT_MAX_IDLE_TIME_MILLIS;
            newPoolConfig = new PoolConfig<W, R>(maxIdleTimeMillis,
                                                 Observable.timer(maxIdleTimeMillis, TimeUnit.MILLISECONDS),
                                                 new MaxConnectionsBasedStrategy(maxConnections),
                                                 newDefaultIdleConnectionsHolder(null));
        }
        final ClientState<W, R> toReturn = new ClientState<W, R>(this, newPoolConfig);
        copyOrCreatePooledConnectionFactory(toReturn);
        return toReturn;
    }

    public ClientState<W, R> maxIdleTimeoutMillis(long maxIdleTimeoutMillis) {
        PoolConfig<W, R> newPoolConfig;
        if (null != poolConfig) {
            newPoolConfig = poolConfig.maxIdleTimeoutMillis(maxIdleTimeoutMillis);
        } else {
            long maxIdleTimeMillis = DEFAULT_MAX_IDLE_TIME_MILLIS;
            newPoolConfig = new PoolConfig<W, R>(maxIdleTimeMillis,
                                                 Observable.timer(maxIdleTimeMillis, TimeUnit.MILLISECONDS),
                                                 new MaxConnectionsBasedStrategy(),
                                                 newDefaultIdleConnectionsHolder(null));
        }
        final ClientState<W, R> toReturn = new ClientState<W, R>(this, newPoolConfig);
        copyOrCreatePooledConnectionFactory(toReturn);
        return toReturn;
    }

    public ClientState<W, R> connectionPoolLimitStrategy(PoolLimitDeterminationStrategy strategy) {
        PoolConfig<W, R> newPoolConfig;
        if (null != poolConfig) {
            newPoolConfig = poolConfig.limitDeterminationStrategy(strategy);
        } else {
            long maxIdleTimeMillis = DEFAULT_MAX_IDLE_TIME_MILLIS;
            newPoolConfig = new PoolConfig<W, R>(maxIdleTimeMillis,
                                                 Observable.timer(maxIdleTimeMillis, TimeUnit.MILLISECONDS), strategy,
                                                 newDefaultIdleConnectionsHolder(null));
        }
        final ClientState<W, R> toReturn = new ClientState<W, R>(this, newPoolConfig);
        copyOrCreatePooledConnectionFactory(toReturn);
        return toReturn;
    }

    public ClientState<W, R> idleConnectionCleanupTimer(Observable<Long> idleConnectionCleanupTimer) {
        PoolConfig<W, R> newPoolConfig;
        if (null != poolConfig) {
            newPoolConfig = poolConfig.idleConnectionsCleanupTimer(idleConnectionCleanupTimer);
        } else {
            newPoolConfig = new PoolConfig<W, R>(DEFAULT_MAX_IDLE_TIME_MILLIS, idleConnectionCleanupTimer,
                                                 new MaxConnectionsBasedStrategy(),
                                                 newDefaultIdleConnectionsHolder(null));
        }
        final ClientState<W, R> toReturn = new ClientState<W, R>(this, newPoolConfig);
        copyOrCreatePooledConnectionFactory(toReturn);
        return toReturn;
    }

    public ClientState<W, R> idleConnectionsHolder(IdleConnectionsHolder<W, R> holder) {
        PoolConfig<W, R> newPoolConfig;
        if (null != poolConfig) {
            newPoolConfig = poolConfig.idleConnectionsHolder(holder);
        } else {
            newPoolConfig = new PoolConfig<W, R>(DEFAULT_MAX_IDLE_TIME_MILLIS,
                                                 Observable.timer(DEFAULT_MAX_IDLE_TIME_MILLIS, TimeUnit.MILLISECONDS),
                                                 new MaxConnectionsBasedStrategy(), holder);
        }
        final ClientState<W, R> toReturn = new ClientState<W, R>(this, newPoolConfig);
        copyOrCreatePooledConnectionFactory(toReturn);
        return toReturn;
    }

    public ClientState<W, R> noIdleConnectionCleanup() {
        return idleConnectionCleanupTimer(Observable.<Long>never());
    }

    public ClientState<W, R> noConnectionPooling() {
        final ClientState<W, R> toReturn = new ClientState<W, R>(this, (PoolConfig<W, R>)null);
        toReturn.connectionFactory = new UnpooledClientConnectionFactory<>(toReturn);
        return toReturn;
    }

    public ClientState<W, R> remoteAddress(SocketAddress newAddress) {
        return new ClientState<W, R>(this, newAddress);
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public ClientConnectionFactory<W, R> getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Returns connection pool configuration, if any.
     *
     * @return Connection pool configuration, if exists, else {@code null}
     */
    public PoolConfig<W, R> getPoolConfig() {
        return poolConfig;
    }

    public MetricEventsSubject<ClientMetricsEvent<?>> getEventsSubject() {
        return eventsSubject;
    }

    public static <WW, RR> ClientState<WW, RR> create() {
        return create(RxNetty.getRxEventLoopProvider().globalClientEventLoop(true), NioSocketChannel.class);
    }

    public static <WW, RR> ClientState<WW, RR> create(SocketAddress remoteAddress) {
        return create(RxNetty.getRxEventLoopProvider().globalClientEventLoop(true), NioSocketChannel.class, remoteAddress);
    }

    public static <WW, RR> ClientState<WW, RR> create(EventLoopGroup group, Class<? extends Channel> channelClass) {
        return create(group, channelClass, new InetSocketAddress("127.0.0.1", 80));
    }

    public static <WW, RR> ClientState<WW, RR> create(EventLoopGroup group, Class<? extends Channel> channelClass,
                                                      SocketAddress remoteAddress) {
        final ClientState<WW, RR> toReturn = new ClientState<WW, RR>(group, channelClass, remoteAddress);
        toReturn.connectionFactory = new UnpooledClientConnectionFactory<>(toReturn);
        return toReturn;
    }

    public static <WW, RR> ClientState<WW, RR> create(ClientState<WW, RR> state,
                                                      ClientConnectionFactory<WW, RR> connectionFactory) {
        state.connectionFactory = connectionFactory;
        return state;
    }

    /*package private. Should not leak as it is mutable*/ Bootstrap getBootstrap() {
        return clientBootstrap;
    }

    private IdleConnectionsHolder<W, R> newDefaultIdleConnectionsHolder(final IdleConnectionsHolder<W, R> template) {
        if (clientBootstrap.group() instanceof PreferCurrentEventLoopGroup) {
            PreferCurrentEventLoopGroup pGroup = (PreferCurrentEventLoopGroup) clientBootstrap.group();
            return new PreferCurrentEventLoopHolder<W, R>(pGroup, eventsSubject,
                                                          new IdleConnectionsHolderFactoryImpl<>(template));
        } else {
            return new FIFOIdleConnectionsHolder<>(eventsSubject);
        }
    }

    private void copyOrCreatePooledConnectionFactory(ClientState<W, R> toReturn) {
        if (connectionFactory instanceof PooledClientConnectionFactory) {
            toReturn.connectionFactory = connectionFactory.copy(toReturn);
        } else {
            toReturn.connectionFactory = new PooledClientConnectionFactoryImpl<W, R>(toReturn);
        }
    }

    private ClientState<W, R> copyBootstrapOnly() {
        ClientState<W, R> toReturn = new ClientState<W, R>(this, clientBootstrap.clone());
        // Connection factory depends on bootstrap, so it should change whenever bootstrap changes.
        toReturn.connectionFactory = connectionFactory.copy(toReturn);
        return toReturn;
    }

    private <WW, RR> ClientState<WW, RR> copy() {
        ClientState<WW, RR> toReturn = new ClientState<WW, RR>(this, detachedPipeline.copy());
        // Connection factory depends on bootstrap, so it should change whenever bootstrap changes.
        toReturn.connectionFactory = connectionFactory.copy(toReturn);
        return toReturn;
    }

    @SuppressWarnings("unchecked")
    private <WW, RR> ClientState<WW, RR> cast() {
        return (ClientState<WW, RR>) this;
    }

    private class IdleConnectionsHolderFactoryImpl<WW, RR> implements IdleConnectionsHolderFactory<WW, RR> {

        private final IdleConnectionsHolder<WW, RR> template;

        public IdleConnectionsHolderFactoryImpl(IdleConnectionsHolder<WW, RR> template) {
            this.template = template;
        }

        @Override
        public <WWW, RRR> IdleConnectionsHolderFactory<WWW, RRR> copy(ClientState<WWW, RRR> newState) {
            return new IdleConnectionsHolderFactoryImpl<>(template.copy(newState));
        }

        @Override
        public IdleConnectionsHolder<WW, RR> call() {
            if (null == template) {
                return new FIFOIdleConnectionsHolder<>(eventsSubject);
            } else {
                return template.copy(ClientState.this.<WW, RR>cast());
            }
        }
    }
}
