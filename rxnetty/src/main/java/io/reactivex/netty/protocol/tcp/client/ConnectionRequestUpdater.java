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
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import rx.functions.Action1;

import java.util.concurrent.TimeUnit;

/**
 * An instance of this class can only be obtained from {@link ConnectionRequest#newUpdater()} and is an
 * optimization to reduce the creation of intermediate {@link ConnectionRequest} objects while doing multiple mutations
 * on the request.
 *
 * Semantically, the operations here are exactly the same as those in {@link ConnectionRequest}, the updates are applied
 * (by invoking {@link #update()}) as a single batch to create a new {@link ConnectionRequest} instance.
 *
 * @param <W> The type of the objects that are written to the connection created by this request.
 * @param <R> The type of objects that are read from the connection created by this request.
 */
public abstract class ConnectionRequestUpdater<W, R> {

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * action to configure all the connections created by the newly created client instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Enables read timeout for all the connection created by this request.
     *
     * @param timeOut Read timeout duration.
     * @param timeUnit Read timeout time unit.
     *
     * @return This updater.
     */
    public abstract ConnectionRequestUpdater<W, R> readTimeOut(int timeOut, TimeUnit timeUnit);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLogginLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                        logging is enabled at this level for {@link LoggingHandler}
     *
     * @return This updater.
     */
    public abstract ConnectionRequestUpdater<W, R> enableWireLogging(LogLevel wireLogginLevel);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code sslEngineFactory} for all secured connections created by the newly created client instance.
     *
     * @param sslEngineFactory {@link SSLEngineFactory} for all secured connections created by the newly created client
     *                                                 instance.
     *
     * @return This updater.
     */
    public abstract ConnectionRequestUpdater<W, R> sslEngineFactory(SSLEngineFactory sslEngineFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> addChannelHandlerFirst(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> addChannelHandlerFirst(EventExecutorGroup group,
                                                                                     String name,
                                                                                     ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR>  addChannelHandlerLast(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                             ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(String, String, ChannelHandler)}
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                               ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                               String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(String, String, ChannelHandler)}
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                                                     ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <WW, RR> ConnectionRequestUpdater<WW, RR> addChannelHandlerAfter(EventExecutorGroup group,
                                                                                     String baseName,
                                                                                     String name,
                                                                                     ChannelHandler handler);
    
    /**
     * Applies all changes done via this updater to the original snapshot of state from {@link ConnectionRequest} and
     * creates a new {@link ConnectionRequest} instance.
     *
     * @return New instance of {@link ConnectionRequest} with all changes done via this updater.
     */
    public abstract ConnectionRequest<W, R> update();
}
