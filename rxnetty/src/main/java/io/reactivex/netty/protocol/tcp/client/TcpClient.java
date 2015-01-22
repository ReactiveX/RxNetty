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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import rx.functions.Action1;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A TCP client for creating TCP connections.
 *
 * <h2>Immutability</h2>
 * An instance of this client is immutable and all mutations produce a new client instance. For this reason it is
 * recommended that the mutations are done during client creation and not during connection creation to avoid repeated
 * object creation overhead.
 *
 * @param <I> The type of objects written to this client.
 * @param <O> The type of objects read from this client.
 */
public abstract class TcpClient<I, O> {

    /**
     * Creates a new {@link ConnectionRequest} which should be subscribed to actually connect to the target server.
     *
     * @return A new {@link ConnectionRequest} which either can be subscribed directly or altered in various ways
     * before subscription.
     */
    public abstract ConnectionRequest<I, O> createConnectionRequest();

    /**
     * Creates a new {@link ConnectionRequest} which should be subscribed to actually connect to the target server.
     * This method overrides the default host and port configured for this client.
     *
     * @param host Target host to connect.
     * @param port Port on the host to connect.
     *
     * @return A new {@link ConnectionRequest} which either can be subscribed directly or altered in various ways
     * before subscription.
     */
    public abstract ConnectionRequest<I, O> createConnectionRequest(String host, int port);

    /**
     * Creates a new client instances, inheriting all configurations from this client and adding a
     * {@link ChannelOption} for the connections created by the newly created client instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <T> TcpClient<I, O> channelOption(ChannelOption<T> option, T value);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> addChannelHandlerFirst(String name, ChannelHandler handler);

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
     * @param handler  the handler to append
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                      ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO>  addChannelHandlerLast(String name, ChannelHandler handler);

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
     * @param handler  the handler to append
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                     ChannelHandler handler);

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
     * @param handler Handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                       ChannelHandler handler);

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
     * @param handler  the handler to append
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                       String name, ChannelHandler handler);

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
     * @param handler Handler instance to add.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                                      ChannelHandler handler);

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
     * @param handler  the handler to append
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                      String name, ChannelHandler handler);

    /**
     * Removes the {@link ChannelHandler} with the passed {@code name} from the {@link ChannelPipeline} for all
     * connections created by this client.
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> removeHandler(String name);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * action to configure all the connections created by the newly created client instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract <II, OO> TcpClient<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * eventLoopGroup for all the connections created by the newly created client instance.
     *
     * @param eventLoopGroup {@link EventLoopGroup} to use.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> eventLoop(EventLoopGroup eventLoopGroup);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code maxConnections} as the maximum number of concurrent connections created by the newly created client instance.
     *
     * @param maxConnections Maximum number of concurrent connections to be created by this client.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> maxConnections(int maxConnections);

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
    public abstract TcpClient<I, O> withIdleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code limitDeterminationStrategy} as the strategy to control the maximum concurrent connections created by the
     * newly created client instance.
     *
     * @param limitDeterminationStrategy Strategy to control the maximum concurrent connections created by the
     * newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> withConnectionPoolLimitStrategy(PoolLimitDeterminationStrategy limitDeterminationStrategy);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code poolIdleCleanupScheduler} for detecting and cleaning idle connections by the newly created client instance.
     *
     * @param poolIdleCleanupScheduler Scheduled to schedule idle connections cleanup.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> withPoolIdleCleanupScheduler(ScheduledExecutorService poolIdleCleanupScheduler);

    /**
     * Creates a new client instances, inheriting all configurations from this client and disabling idle connection
     * cleanup for the newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> withNoIdleConnectionCleanup();

    /**
     * Creates a new client instances, inheriting all configurations from this client and disabling connection
     * pooling for the newly created client instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> withNoConnectionPooling();

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLoggingLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                         logging is enabled at this level for {@link LoggingHandler}
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> enableWireLogging(LogLevel wireLoggingLevel);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code sslEngineFactory} for all secured connections created by the newly created client instance.
     *
     * @param sslEngineFactory {@link SSLEngineFactory} for all secured connections created by the newly created client
     *                                                 instance.
     *
     * @return A new {@link TcpClient} instance.
     */
    public abstract TcpClient<I, O> withSslEngineFactory(SSLEngineFactory sslEngineFactory);
}
