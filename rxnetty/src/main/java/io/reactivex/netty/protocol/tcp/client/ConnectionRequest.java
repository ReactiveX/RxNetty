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
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.util.concurrent.TimeUnit;

/**
 * A TCP connection request created via {@link TcpClient#createConnectionRequest()}.
 *
 * <h2>Mutations</h2>
 *
 * All mutations to this request that creates a brand new instance.
 *
 * <h2> Inititating connections</h2>
 *
 * A new connection is initiated every time {@link ConnectionRequest#subscribe()} is called and is the only way of
 * creating connections.
 *
 * @param <W> The type of the objects that are written to the connection created by this request.
 * @param <R> The type of objects that are read from the connection created by this request.
 */
public abstract class ConnectionRequest<W, R> extends Observable<Connection<R, W>> {

    protected ConnectionRequest(OnSubscribe<Connection<R, W>> f) {
        super(f);
    }

    /**
     * Enables read timeout for all the connection created by this request.
     *
     * @param timeOut Read timeout duration.
     * @param timeUnit Read timeout time unit.
     *
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract ConnectionRequest<W, R> readTimeOut(int timeOut, TimeUnit timeUnit);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLogginLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                        logging is enabled at this level for {@link LoggingHandler}
     *
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract ConnectionRequest<W, R> enableWireLogging(LogLevel wireLogginLevel);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by
     * this request. The specified handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
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
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(EventExecutorGroup group,
                                                                              String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR>  addChannelHandlerLast(String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
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
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                             Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
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
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                               Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
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
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(EventExecutorGroup group,
                                                                               String baseName,
                                                                               String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
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
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for the connections created by this request. The specified
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
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(EventExecutorGroup group,
                                                                              String baseName,
                                                                              String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * action to configure all the connections created by the newly created request instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new instance of the {@link ConnectionRequest} sharing all existing state from this request.
     */
    public abstract <WW, RR> ConnectionRequest<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Creates a new server instances, inheriting all configurations from this request and using the passed
     * {@code sslEngineFactory} for all secured connections created by the newly created client instance.
     *
     * If the {@link SSLEngine} instance can be statically, created, {@link #secure(SSLEngine)} can be used.
     *
     * @param sslEngineFactory Factory for all secured connections created by the newly created client instance.
     *
     * @return A new {@link ConnectionRequest} instance.
     */
    public abstract ConnectionRequest<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory);

    /**
     * Creates a new request instance, inheriting all configurations from this client and using the passed
     * {@code sslEngine} for all secured connections created by the newly created request instance.
     *
     * If the {@link SSLEngine} instance can not be statically, created, {@link #secure(Func1)} )} can be used.
     *
     * @param sslEngine {@link SSLEngine} for all secured connections created by the newly created request instance.
     *
     * @return A new {@link ConnectionRequest} instance.
     */
    public abstract ConnectionRequest<W, R> secure(SSLEngine sslEngine);

    /**
     * Creates a new client instance, inheriting all configurations from this request and using the passed
     * {@code sslCodec} for all secured connections created by the newly created request instance.
     *
     * This is required only when the {@link SslHandler} used by {@link SslCodec} is to be modified before adding to
     * the {@link ChannelPipeline}. For most of the cases, {@link #secure(Func1)} or {@link #secure(SSLEngine)} will be
     * enough.
     *
     * @param sslCodec {@link SslCodec} for all secured connections created by the newly created request instance.
     *
     * @return A new {@link ConnectionRequest} instance.
     */
    public abstract ConnectionRequest<W, R> secure(SslCodec sslCodec);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using a trust-all
     * {@link TrustManagerFactory}for all secured connections created by the newly created request
     * instance.
     *
     * <b>This is only for testing and should not be used for real production clients.</b>
     *
     * @return A new {@link ConnectionRequest} instance.
     */
    public abstract ConnectionRequest<W, R> unsafeSecure();
}
