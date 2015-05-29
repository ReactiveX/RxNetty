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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.protocol.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * A TCP client for creating TCP connections.
 *
 * <h2>Immutability</h2>
 * An instance of this client is immutable and all mutations produce a new client instance. For this reason it is
 * recommended that the mutations are done during client creation and not during connection creation to avoid repeated
 * object creation overhead.
 *
 * @param <W> The type of objects written to this client.
 * @param <R> The type of objects read from this client.
 */
public abstract class TcpClient<W, R> implements EventSource<TcpClientEventListener> {

    public static final String TCP_CLIENT_NO_NAME = "TcpClient-no-name";

    /**
     * Creates a new {@link ConnectionRequest} which should be subscribed to actually connect to the target server.
     *
     * @return A new {@link ConnectionRequest} which either can be subscribed directly or altered in various ways
     * before subscription.
     */
    public abstract ConnectionRequest<W, R> createConnectionRequest();

    /**
     * Creates a new client instances, inheriting all configurations from this client and adding a
     * {@link ChannelOption} for the connections created by the newly created client instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <T> TcpClient<W, R> channelOption(ChannelOption<T> option, T value);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enables read timeout for all
     * the connection created by this client.
     *
     * @param timeOut Read timeout duration.
     * @param timeUnit Read timeout time unit.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> readTimeOut(int timeOut, TimeUnit timeUnit);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR>  addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                     Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                       String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                      String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * action to configure all the connections created by the newly created client instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <WW, RR> TcpClient<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code maxConnections} as the maximum number of concurrent connections created by the newly created client instance.
     *
     * @param maxConnections Maximum number of concurrent connections to be created by this client.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> maxConnections(int maxConnections);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code idleConnectionsTimeoutMillis} as the time elapsed before an idle connections will be closed by the newly
     * created client instance.
     *
     * @param idleConnectionsTimeoutMillis Time elapsed before an idle connections will be closed by the newly
     * created client instance
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> idleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code strategy} as the strategy to control the maximum concurrent connections created by the
     * newly created client instance.
     *
     * @param strategy Strategy to control the maximum concurrent connections created by the
     * newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> connectionPoolLimitStrategy(PoolLimitDeterminationStrategy strategy);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code idleConnectionCleanupTimer} for trigerring detection and cleanup of idle connections by the newly created
     * client instance.
     *
     * @param idleConnectionCleanupTimer Timer to trigger idle connections cleanup.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> idleConnectionCleanupTimer(Observable<Long> idleConnectionCleanupTimer);

    /**
     * Creates a new client instances, inheriting all configurations from this client and disabling idle connection
     * cleanup for the newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> noIdleConnectionCleanup();

    /**
     * Creates a new client instances, inheriting all configurations from this client and disabling connection
     * pooling for the newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> noConnectionPooling();

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code factory} for the newly created client instance.
     *
     * @param factory Connection factory to use for the new client.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> connectionFactory(Func1<ClientState<W, R>, ClientConnectionFactory<W, R>> factory);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLoggingLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                         logging is enabled at this level for {@link LoggingHandler}
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> enableWireLogging(LogLevel wireLoggingLevel);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using the passed
     * {@code sslEngineFactory} for all secured connections created by the newly created client instance.
     *
     * If the {@link SSLEngine} instance can be statically, created, {@link #secure(SSLEngine)} can be used.
     *
     * @param sslEngineFactory Factory for all secured connections created by the newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using the passed
     * {@code sslEngine} for all secured connections created by the newly created client instance.
     *
     * If the {@link SSLEngine} instance can not be statically, created, {@link #secure(Func1)} )} can be used.
     *
     * @param sslEngine {@link SSLEngine} for all secured connections created by the newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> secure(SSLEngine sslEngine);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using the passed
     * {@code sslCodec} for all secured connections created by the newly created client instance.
     *
     * This is required only when the {@link SslHandler} used by {@link SslCodec} is to be modified before adding to
     * the {@link ChannelPipeline}. For most of the cases, {@link #secure(Func1)} or {@link #secure(SSLEngine)} will be
     * enough.
     *
     * @param sslCodec {@link SslCodec} for all secured connections created by the newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> secure(SslCodec sslCodec);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using a trust-all
     * {@link TrustManagerFactory}for all secured connections created by the newly created client instance.
     *
     * <b>This is only for testing and should not be used for real production clients.</b>
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<W, R> unsafeSecure();

    /**
     * Returns the event publisher for this client.
     *
     * @return The event publisher for this client.
     */
    public abstract TcpClientEventPublisher getEventPublisher();

    public static TcpClient<ByteBuf, ByteBuf> newClient(String host, int port) {
        return newClient(TCP_CLIENT_NO_NAME, host, port);
    }

    public static TcpClient<ByteBuf, ByteBuf> newClient(String name, String host, int port) {
        return new TcpClientImpl<>(name, new InetSocketAddress(host, port));
    }

    public static TcpClient<ByteBuf, ByteBuf> newClient(EventLoopGroup eventLoopGroup,
                                                        Class<? extends Channel> channelClass, String host, int port) {
        return newClient(eventLoopGroup, channelClass, TCP_CLIENT_NO_NAME, host, port);
    }

    public static TcpClient<ByteBuf, ByteBuf> newClient(EventLoopGroup eventLoopGroup,
                                                        Class<? extends Channel> channelClass, String name, String host,
                                                        int port) {
        return new TcpClientImpl<>(name, eventLoopGroup, channelClass, new InetSocketAddress(host, port));
    }

    public static TcpClient<ByteBuf, ByteBuf> newClient(SocketAddress remoteAddress) {
        return newClient(TCP_CLIENT_NO_NAME, remoteAddress);
    }

    public static TcpClient<ByteBuf, ByteBuf> newClient(String name, SocketAddress remoteAddress) {
        return new TcpClientImpl<>(name, remoteAddress);
    }

    public static TcpClient<ByteBuf, ByteBuf> newClient(EventLoopGroup eventLoopGroup,
                                                        Class<? extends Channel> channelClass,
                                                        SocketAddress remoteAddress) {
        return newClient(eventLoopGroup, channelClass, TCP_CLIENT_NO_NAME, remoteAddress);
    }

    public static TcpClient<ByteBuf, ByteBuf> newClient(EventLoopGroup eventLoopGroup,
                                                        Class<? extends Channel> channelClass, String name,
                                                        SocketAddress remoteAddress) {
        return new TcpClientImpl<>(name, eventLoopGroup, channelClass, remoteAddress);
    }
}
