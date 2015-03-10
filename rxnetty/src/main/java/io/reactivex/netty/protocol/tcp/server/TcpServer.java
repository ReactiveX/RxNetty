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
package io.reactivex.netty.protocol.tcp.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.metrics.MetricEventsPublisher;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import io.reactivex.netty.server.ServerMetricsEvent;
import rx.functions.Action1;
import rx.functions.Func0;

import java.util.concurrent.TimeUnit;

/**
 * A TCP server.
 *
 * <h2>Immutability</h2>
 * An instance of this server is immutable and all mutations produce a new server instance.
 *
 * @param <R> The type of objects read from this server.
 * @param <W> The type of objects written to this server.
 *
 * @author Nitesh Kant
 */
public abstract class TcpServer<R, W> implements MetricEventsPublisher<ServerMetricsEvent<?>> {

    /**
     * Creates a new server instance, inheriting all configurations from this server and adding a
     * {@link ChannelOption} for the server socket created by the newly created server instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract <T> TcpServer<R, W> channelOption(ChannelOption<T> option, T value);

    /**
     * Creates a new server instance, inheriting all configurations from this server and adding a
     * {@link ChannelOption} for the client socket created by the newly created server instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract <T> TcpServer<R, W> clientChannelOption(ChannelOption<T> option, T value);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server.
     * The specified handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)}
     * will be more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group The {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name The name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW>  addChannelHandlerLast(String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The specified
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
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                     Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The specified
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
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> addChannelHandlerBefore(String baseName, String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The specified
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
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                       String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The specified
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
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> addChannelHandlerAfter(String baseName, String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The specified
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
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                      String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * action to configure all the connections created by the newly created client instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract <RR, WW> TcpServer<RR, WW> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code sslEngineFactory} for all secured connections created by the newly created client instance.
     *
     * @param sslEngineFactory {@link SSLEngineFactory} for all secured connections created by the newly created client
     *                                                 instance.
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract TcpServer<R, W> sslEngineFactory(SSLEngineFactory sslEngineFactory);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLoggingLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                         logging is enabled at this level for {@link io.netty.handler.logging.LoggingHandler}
     *
     * @return A new {@link TcpServer} instance.
     */
    public abstract TcpServer<R, W> enableWireLogging(LogLevel wireLoggingLevel);

    /**
     * Returns the port at which this server is running.
     *
     * For servers using ephemeral ports, this would return the actual port used, only after the server is started.
     *
     * @return The port at which this server is running.
     */
    public abstract int getServerPort();

    /**
     * Starts this server and waits till the server is shutdown. This will block the caller thread till the time the
     * server is shutdown. If blocking the caller is not required, use {@link #start(ConnectionHandler)}
     *
     * @param connectionHandler Connection handler that will handle any new client connections to this server.
     */
    public abstract void startAndWait(ConnectionHandler<R, W> connectionHandler);

    /**
     * Starts this server.
     *
     * @param connectionHandler Connection handler that will handle any new client connections to this server.
     *
     * @return This server.
     */
    public abstract TcpServer<R, W> start(ConnectionHandler<R, W> connectionHandler);

    /**
     * Shutdown this server and waits till the server socket is closed.
     */
    public abstract void shutdown();

    /**
     * Waits for the shutdown of this server.
     *
     * <b>This does not actually shutdown the server.</b> It just waits for some other action to shutdown.
     */
    public abstract void waitTillShutdown();

    /**
     * Waits for the shutdown of this server, waiting a maximum of the passed duration.
     *
     * <b>This does not actually shutdown the server.</b> It just waits for some other action to shutdown.
     *
     * @param duration Duration to wait for shutdown.
     * @param timeUnit Timeunit for the duration to wait for shutdown.
     */
    public abstract void waitTillShutdown(long duration, TimeUnit timeUnit);
}
