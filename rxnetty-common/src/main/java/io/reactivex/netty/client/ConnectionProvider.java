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
package io.reactivex.netty.client;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.resolver.DefaultNameResolverGroup;
import io.netty.resolver.NameResolverGroup;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.pool.PooledConnectionProvider;
import rx.Observable;
import rx.annotations.Beta;
import rx.functions.Action0;
import rx.functions.Func1;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A connection provider to be used by clients.
 * <p>
 * There are two distinct kind of connection providers, viz.,
 * <p>
 * <h2>Connection provider for a single host</h2>
 * <p>
 * Such a connection provider can be created using {@code forHost} methods ({@link #forHost(SocketAddress)} ,
 * {@link #forHost(SocketAddress, EventLoopGroup, Class)}, {@link #forHost(String, int)},
 * {@link #forHost(SocketAddress, EventLoopGroup, Class)}).
 * <p>
 * <h2>Connection provider for a pool of hosts</h2>
 * <p>
 * Such a connection provider would be created by using the create methods ({@link #create(Func1)},
 * {@link #create(Func1, EventLoopGroup, Class)}) because such a {@link ConnectionProvider} requires a
 * {@link ConnectionFactory} to create {@link Connection}s per target server.
 *
 * @param <W> Type of object that is written to the connections created by this provider.
 * @param <R> Type of object that is read from the connections created by this provider.
 * @see PooledConnectionProvider
 */
@Beta
public abstract class ConnectionProvider<W, R> {

    private final Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> creationFunc;
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends Channel> channelClass;
    private Observable<Void> shutdownObservable;/*Guarded by this*/
    private boolean shutdownCalled; /*Guarded by this*/
    private volatile boolean shutdown;
    private final NameResolverGroup<?> nameResolver;

    private ConnectionProvider(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> creationFunc,
                               EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass,
                               NameResolverGroup<?> nameResolver) {
        this.creationFunc = creationFunc;
        this.eventLoopGroup = eventLoopGroup;
        this.channelClass = channelClass;
        this.nameResolver = nameResolver;
    }

    /**
     * Creates a new {@link ConnectionProvider} using the passed {@code connectionFactory}
     *
     * @param connectionFactory Connection factory.
     */
    protected ConnectionProvider(@SuppressWarnings("unused") ConnectionFactory<W, R> connectionFactory) {
        this(new UninitializedCreationFunc<W, R>(), RxNetty.getRxEventLoopProvider().globalClientEventLoop(true),
                defaultSocketChannelClass(), DefaultNameResolverGroup.INSTANCE);
    }

    /**
     * Creates a new {@link ConnectionProvider} using the passed {@code connectionFactory} and {@code nameResolver}
     *
     * @param connectionFactory Connection factory.
     */
    protected ConnectionProvider(@SuppressWarnings("unused") ConnectionFactory<W, R> connectionFactory, NameResolverGroup nameResolver) {
        this(new UninitializedCreationFunc<W, R>(), RxNetty.getRxEventLoopProvider().globalClientEventLoop(true),
                defaultSocketChannelClass(), nameResolver);
    }

    /**
     * Creates a new connection request, each subscription to which will emit at most one {@link Connection}.
     *
     * @return An {@code Observable}, every subscription to which will emit a connection.
     */
    public abstract ConnectionObservable<R, W> nextConnection();

    /**
     * Shutdown this connection provider.
     *
     * <b>This method is idempotent, so can be called multiple times without any side-effects</b>
     *
     * Implementations can override {@link #doShutdown()} to implement any shutdown logic, {@link #doShutdown()} gets
     * called exactly once.
     *
     * @return {@code Observable} which completes when the factory is successfully shutdown.
     */
    public final Observable<Void> shutdown() {
        Observable<Void> _toReturn;
        synchronized (this) {
            if (!shutdownCalled) {
                shutdownCalled = true;
                shutdownObservable = doShutdown().doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        shutdown = true; /*Only one subscription as it uses cache*/
                    }
                }).cache();
            }
            _toReturn = shutdownObservable;
        }
        return _toReturn;
    }

    protected boolean isShutdown() {
        return shutdown;
    }

    /**
     * This method can be overridden to implement any provider specific initialization. The method gets called exactly
     * once, irrespective of how many times {@link #shutdown()} is called.
     *
     * @return An {@code Observable} representing the status of the start.
     */
    protected Observable<Void> doShutdown() {
        return Observable.empty();
    }

    /**
     * Creates a concrete {@link ConnectionProvider} for the passed {@code host} and {@code port}
     *
     * @param host The target server hostname.
     * @param port The target server port.
     *
     * @return A concrete {@link ConnectionProvider} for the passed {@code host}.
     */
    public static <W, R> ConnectionProvider<W, R> forHost(final String host, final int port) {
        return forHost(host, port, RxNetty.getRxEventLoopProvider().globalClientEventLoop(true),
                       defaultSocketChannelClass());
    }

    /**
     * Creates a concrete {@link ConnectionProvider} for the passed {@code host} and {@code port}
     *
     * @param host The target server hostname.
     * @param port The target server port.
     * @param eventLoopGroup Event loop group to use.
     * @param channelClass Class of the channel.
     *
     * @return A concrete {@link ConnectionProvider} for the passed {@code host}.
     */
    public static <W, R> ConnectionProvider<W, R> forHost(final String host, final int port,
                                                          EventLoopGroup eventLoopGroup,
                                                          Class<? extends Channel> channelClass) {
        return forHost(new InetSocketAddress(host, port), eventLoopGroup, channelClass,
                DefaultNameResolverGroup.INSTANCE);
    }

    /**
     * Creates a concrete {@link ConnectionProvider} for the passed {@code host}
     *
     * @param host The target server address.
     *
     * @return A concrete {@link ConnectionProvider} for the passed {@code host}.
     */
    public static <W, R> ConnectionProvider<W, R> forHost(final SocketAddress host) {
        return forHost(host, RxNetty.getRxEventLoopProvider().globalClientEventLoop(true),
                defaultSocketChannelClass(), DefaultNameResolverGroup.INSTANCE);
    }

    /**
     * Creates a concrete {@link ConnectionProvider} for the passed {@code host}
     *
     * @param host The target server address.
     * @return A concrete {@link ConnectionProvider} for the passed {@code host}.
     */
    public static <W, R> ConnectionProvider<W, R> forHost(final SocketAddress host, NameResolverGroup nameResolver) {
        return forHost(host, RxNetty.getRxEventLoopProvider().globalClientEventLoop(true),
                defaultSocketChannelClass(), nameResolver);
    }

    /**
     * Creates a concrete {@link ConnectionProvider} for the passed {@code host}
     *
     * @param host           The target server address.
     * @param eventLoopGroup Event loop group to use.
     * @param channelClass   Class of the channel.
     * @param nameResolver Domain name resolver.
     *
     * @return A concrete {@link ConnectionProvider} for the passed {@code host}.
     */
    public static <W, R> ConnectionProvider<W, R> forHost(final SocketAddress host, EventLoopGroup eventLoopGroup,
                                                          Class<? extends Channel> channelClass, NameResolverGroup nameResolver) {
        return create(new Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>>() {
            @Override
            public ConnectionProvider<W, R> call(final ConnectionFactory<W, R> connectionFactory) {

                return new ConnectionProvider<W, R>(connectionFactory) {
                    @Override
                    public ConnectionObservable<R, W> nextConnection() {
                        return connectionFactory.newConnection(host);
                    }
                };
            }
        }, eventLoopGroup, channelClass, nameResolver);
    }

    /**
     * Creates a raw {@link ConnectionProvider} using the provided function to create actual {@link ConnectionProvider}
     * instance.
     *
     * @param func A function to create a concrete {@link ConnectionProvider} using the passed {@link ConnectionFactory}
     *
     * @return A raw connection factory that will use the passed function to create concrete {@link ConnectionProvider}
     * instance.
     */
    public static <W, R> ConnectionProvider<W, R> create(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> func) {
        return create(func, RxNetty.getRxEventLoopProvider().globalClientEventLoop(true),
                defaultSocketChannelClass(), DefaultNameResolverGroup.INSTANCE);
    }


    /**
     * Creates a raw {@link ConnectionProvider} using the provided function to create actual {@link ConnectionProvider}
     * instance.
     *
     * @param func A function to create a concrete {@link ConnectionProvider} using the passed {@link ConnectionFactory}
     *
     * @return A raw connection factory that will use the passed function to create concrete {@link ConnectionProvider}
     * instance.
     */
    public static <W, R> ConnectionProvider<W, R> create(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> func, NameResolverGroup nameResolver) {
        return create(func, RxNetty.getRxEventLoopProvider().globalClientEventLoop(true),
                defaultSocketChannelClass(), nameResolver);
    }

    /**
     * Creates a raw {@link ConnectionProvider} using the provided function to create actual {@link ConnectionProvider}
     * instance.
     *
     * @param func           A function to create a concrete {@link ConnectionProvider} using the passed {@link ConnectionFactory}
     * @param eventLoopGroup Event loop group to use.
     * @param channelClass   Class of the channel.
     * @param nameResolver Domain name resolver.
     *
     * @return A raw connection factory that will use the passed function to create concrete {@link ConnectionProvider}
     * instance.
     */
    public static <W, R> ConnectionProvider<W, R> create(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> func,
                                                         EventLoopGroup eventLoopGroup,
                                                         Class<? extends Channel> channelClass, NameResolverGroup nameResolver) {
        return new UninitializedConnectionProvider<>(func, eventLoopGroup, channelClass, nameResolver);
    }

    protected final boolean isEventPublishingEnabled() {
        return !RxNetty.isEventPublishingDisabled();
    }

    /*package private*/ ConnectionProvider<W, R> realize(ConnectionFactory<W, R> connectionFactory) {
        return creationFunc.call(connectionFactory);
    }

    EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    Class<? extends Channel> getChannelClass() {
        return channelClass;
    }

    public NameResolverGroup<?> getNameResolver() {
        return nameResolver;
    }


    private static class UninitializedCreationFunc<W, R>
            implements Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> {

        @Override
        public ConnectionProvider<W, R> call(ConnectionFactory<W, R> connectionFactory) {
            return new UninitializedConnectionProvider<>(connectionFactory);
        }
    }

    private static class UninitializedConnectionProvider<W, R> extends ConnectionProvider<W, R> {

        private UninitializedConnectionProvider(ConnectionFactory<W, R> connectionFactory) {
            super(connectionFactory);
        }

        private UninitializedConnectionProvider(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> func,
                                                EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass, NameResolverGroup nameResolver) {
            super(func, eventLoopGroup, channelClass, nameResolver);
        }

        @Override
        public ConnectionObservable<R, W> nextConnection() {
            return ConnectionObservable.forError(new IllegalStateException("Connection factory not initialized"));
        }
    }

    private static Class<? extends AbstractChannel> defaultSocketChannelClass() {
        return RxNetty.isUsingNativeTransport() ? EpollSocketChannel.class : NioSocketChannel.class;
    }
}
