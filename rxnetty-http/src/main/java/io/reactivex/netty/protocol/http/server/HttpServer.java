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
package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventsListener;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP server.
 *
 * @param <I> The type of objects received as content from a request in this server.
 * @param <O> The type of objects written as content from a response in this server.
 */
public abstract class HttpServer<I, O> implements EventSource<HttpServerEventsListener> {

    /**
     * Creates a new server instance, inheriting all configurations from this server and adding a {@link ChannelOption}
     * for the server socket created by the newly created server instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <T> HttpServer<I, O> channelOption(ChannelOption<T> option, T value);

    /**
     * Creates a new server instance, inheriting all configurations from this server and adding a {@link ChannelOption}
     * for the client socket created by the newly created server instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <T> HttpServer<I, O> clientChannelOption(ChannelOption<T> option, T value);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this
     * server. The specified handler is added at the first position of the pipeline as specified by {@link
     * ChannelPipeline#addFirst(String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerFirst(String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The
     * specified handler is added at the first position of the pipeline as specified by {@link
     * ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group The {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param name The name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The
     * specified handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerLast(String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The
     * specified handler is added at the last position of the pipeline as specified by {@link
     * ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param name the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The
     * specified handler is added before an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addBefore(String, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param baseName the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                        Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The
     * specified handler is added before an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param baseName the name of the existing handler
     * @param name the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                        String name,
                                                                        Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The
     * specified handler is added after an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addAfter(String, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param baseName the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this server. The
     * specified handler is added after an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param baseName the name of the existing handler
     * @param name the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                       String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed action to
     * configure all the connections created by the newly created client instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract <II, OO> HttpServer<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Creates a new server instances, inheriting all configurations from this server and using the passed
     * {@code sslEngineFactory} for all secured connections accepted by the newly created server instance.
     *
     * If the {@link SSLEngine} instance can be statically, created, {@link #secure(SSLEngine)} can be used.
     *
     * @param sslEngineFactory Factory for all secured connections created by the newly created server instance.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract HttpServer<I, O> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory);

    /**
     * Creates a new server instances, inheriting all configurations from this server and using the passed
     * {@code sslEngine} for all secured connections accepted by the newly created server instance.
     *
     * If the {@link SSLEngine} instance can not be statically, created, {@link #secure(Func1)} )} can be used.
     *
     * @param sslEngine {@link SSLEngine} for all secured connections created by the newly created server instance.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract HttpServer<I, O> secure(SSLEngine sslEngine);

    /**
     * Creates a new server instances, inheriting all configurations from this server and using the passed
     * {@code sslCodec} for all secured connections accepted by the newly created server instance.
     *
     * This is required only when the {@link SslHandler} used by {@link SslCodec} is to be modified before adding to
     * the {@link ChannelPipeline}. For most of the cases, {@link #secure(Func1)} or {@link #secure(SSLEngine)} will be
     * enough.
     *
     * @param sslCodec {@link SslCodec} for all secured connections created by the newly created server instance.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract HttpServer<I, O> secure(SslCodec sslCodec);

    /**
     * Creates a new server instances, inheriting all configurations from this server and using a self-signed
     * certificate for all secured connections accepted by the newly created server instance.
     *
     * <b>This is only for testing and should not be used for real production servers.</b>
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract HttpServer<I, O> unsafeSecure();

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLoggingLevel Logging level at which the wire logs will be logged. The wire logging will only be done
     * if logging is enabled at this level for {@link io.netty.handler.logging.LoggingHandler}
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract HttpServer<I, O> enableWireLogging(LogLevel wireLoggingLevel);

    /**
     * According to the <a href="http://tools.ietf.org/html/rfc2145#section-2.3">specification</a>, an HTTP server must
     * send the highest HTTP version response, it is compatible with. Since, all implementations of this server are
     * expected to be HTTP 1.1 compatible, they should always send the response for 1.1. However, in some cases, if
     * desired, this behavior can be overridden to send 1.0 response for 1.0 request. If so desired, the behavior can
     * be enabled/ disabled by this method.
     *
     * @param sendHttp10ResponseFor10Request If {@code true} then sends 1.0 version response for 1.0 request.
     *
     * @return A new {@link HttpServer} instance.
     */
    public abstract HttpServer<I, O> sendHttp10ResponseFor10Request(boolean sendHttp10ResponseFor10Request);

    /**
     * Returns the port at which this server is running.
     * <p/>
     * For servers using ephemeral ports, this would return the actual port used, only after the server is started.
     *
     * @return The port at which this server is running.
     */
    public abstract int getServerPort();

    /**
     * Returns the address at which this server is running.
     *
     * @return The address at which this server is running.
     */
    public abstract SocketAddress getServerAddress();

    /**
     * Starts this server.
     *
     * @param requestHandler Connection handler that will handle any new client connections to this server.
     *
     * @return This server.
     */
    public abstract HttpServer<I, O> start(RequestHandler<I, O> requestHandler);

    /**
     * Shutdown this server and waits till the server socket is closed.
     */
    public abstract void shutdown();

    /**
     * Waits for the shutdown of this server.
     * <p/>
     * <b>This does not actually shutdown the server.</b> It just waits for some other action to shutdown.
     */
    public abstract void awaitShutdown();

    /**
     * Waits for the shutdown of this server, waiting a maximum of the passed duration.
     * <p/>
     * <b>This does not actually shutdown the server.</b> It just waits for some other action to shutdown.
     *
     * @param duration Duration to wait for shutdown.
     * @param timeUnit Timeunit for the duration to wait for shutdown.
     */
    public abstract void awaitShutdown(long duration, TimeUnit timeUnit);

    /**
     * Creates a new server using an ephemeral port. The port used can be found by {@link #getServerPort()}
     *
     * @return A new {@link HttpServer}
     */
    public static HttpServer<ByteBuf, ByteBuf> newServer() {
        return _newServer(TcpServer.newServer(0));
    }

    /**
     * Creates a new server using the passed port.
     *
     * @param port Port for the server. {@code 0} to use ephemeral port.
     * @return A new {@link HttpServer}
     */
    public static HttpServer<ByteBuf, ByteBuf> newServer(int port) {
        return _newServer(TcpServer.newServer(port));
    }

    /**
     * Creates a new server using the passed port.
     *
     * @param port Port for the server. {@code 0} to use ephemeral port.
     * @param eventLoopGroup Eventloop group to be used for server as well as client sockets.
     * @param channelClass The class to be used for server channel.
     *
     * @return A new {@link HttpServer}
     */
    public static HttpServer<ByteBuf, ByteBuf> newServer(int port, EventLoopGroup eventLoopGroup,
                                                         Class<? extends ServerChannel> channelClass) {
        return _newServer(TcpServer.newServer(port, eventLoopGroup, eventLoopGroup, channelClass));
    }

    /**
     * Creates a new server using the passed port.
     *
     * @param port Port for the server. {@code 0} to use ephemeral port.
     * @param serverGroup Eventloop group to be used for server sockets.
     * @param clientGroup Eventloop group to be used for client sockets.
     * @param channelClass The class to be used for server channel.
     *
     * @return A new {@link HttpServer}
     */
    public static HttpServer<ByteBuf, ByteBuf> newServer(int port, EventLoopGroup serverGroup,
                                                         EventLoopGroup clientGroup,
                                                         Class<? extends ServerChannel> channelClass) {
        return _newServer(TcpServer.newServer(port, serverGroup, clientGroup, channelClass));
    }

    /**
     * Creates a new server using the passed port.
     *
     * @param socketAddress Socket address for the server.
     * @return A new {@link HttpServer}
     */
    public static HttpServer<ByteBuf, ByteBuf> newServer(SocketAddress socketAddress) {
        return _newServer(TcpServer.newServer(socketAddress));
    }

    /**
     * Creates a new server using the passed port.
     *
     * @param socketAddress Socket address for the server.
     * @param eventLoopGroup Eventloop group to be used for server as well as client sockets.
     * @param channelClass The class to be used for server channel.
     *
     * @return A new {@link HttpServer}
     */
    public static HttpServer<ByteBuf, ByteBuf> newServer(SocketAddress socketAddress, EventLoopGroup eventLoopGroup,
                                                         Class<? extends ServerChannel> channelClass) {
        return _newServer(TcpServer.newServer(socketAddress, eventLoopGroup, eventLoopGroup, channelClass));
    }

    /**
     * Creates a new server using the passed port.
     *
     * @param socketAddress Socket address for the server.
     * @param serverGroup Eventloop group to be used for server sockets.
     * @param clientGroup Eventloop group to be used for client sockets.
     * @param channelClass The class to be used for server channel.
     *
     * @return A new {@link HttpServer}
     */
    public static HttpServer<ByteBuf, ByteBuf> newServer(SocketAddress socketAddress, EventLoopGroup serverGroup,
                                                         EventLoopGroup clientGroup,
                                                         Class<? extends ServerChannel> channelClass) {
        return _newServer(TcpServer.newServer(socketAddress, serverGroup, clientGroup, channelClass));
    }

    private static HttpServer<ByteBuf, ByteBuf> _newServer(TcpServer<ByteBuf, ByteBuf> tcpServer) {
        return HttpServerImpl.create(tcpServer);
    }
}
