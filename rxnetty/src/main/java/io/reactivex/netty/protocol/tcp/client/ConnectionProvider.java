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

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.pool.PooledConnectionProvider;
import rx.Observable;
import rx.annotations.Beta;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A connection factory to be used by TCP clients or any protocol built on top of TCP (eg: HTTP).
 *
 * There are two distinct kind of connection factories, viz.,
 *
 * <h2>Connection factory for a single host</h2>
 *
 * Such a connection factory can be created using {@code forHost} methods ({@link #forHost(SocketAddress)} ,
 * {@link #forHost(SocketAddress, EventLoopGroup, Class)}, {@link #forHost(String, int)},
 * {@link #forHost(SocketAddress, EventLoopGroup, Class)}).
 *
 * <h2>Connection factory for a pool of hosts</h2>
 *
 * Such a connection factory would be created by using the create methods ({@link #create(Func1)},
 * {@link #create(Func1, EventLoopGroup, Class)}) because every {@link ConnectionProvider} requires
 * {@link BootstrapFactory} to create {@link ConnectionFactory} per target server.
 *
 * <h2>Concrete Implementations</h2>
 *
 * In order to create a concrete {@link ConnectionProvider} implementations, one would require one of the following:
 *
 * <ul>
 <li>{@link BootstrapFactory}: A factory for creating {@link ConnectionFactory} per target server, typically used when
 the connection factory creates connections to multiple target servers.</li>
 <li>{@link ConnectionFactory}: A concrete {@link ConnectionFactory} instance for connecting to a single target server,
 typically used when the connection factory is only connecting to a single target server.</li>
 </ul>
 *
 * @param <W> Type of object that is written to the connections created by this factory.
 * @param <R> Type of object that is read from the connections created by this factory.
 *
 * @see PooledConnectionProvider
 */
@Beta
public abstract class ConnectionProvider<W, R> {

    private final Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> creationFunc;
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends Channel> channelClass;
    private final ReplaySubject<Void> shutdownHook = ReplaySubject.create();
    private volatile boolean shutdown;

    private ConnectionProvider(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> creationFunc,
                               EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass) {
        this.creationFunc = creationFunc;
        this.eventLoopGroup = eventLoopGroup;
        this.channelClass = channelClass;
    }

    /**
     * Creates a new {@link ConnectionProvider} using the passed {@code connectionFactory}
     *
     * @param connectionFactory Connection factory.
     */
    protected ConnectionProvider(@SuppressWarnings("unused") ConnectionFactory<W, R> connectionFactory) {
        this(new UninitializedCreationFunc<W, R>(), RxNetty.getRxEventLoopProvider().globalClientEventLoop(),
             NioSocketChannel.class);
    }

    /**
     * Creates a new connection request, each subscription to which will emit at most one {@link Connection}.
     *
     * @return An {@code Observable}, every subscription to which will emit a connection.
     */
    public abstract ConnectionObservable<R, W> nextConnection();

    /**
     * Starts this connection factory. Implementations can override this to do any initialization tasks. This method
     * would at most be called once per {@link ConnectionProvider} instance.
     *
     * @return {@code Observable} which completes when the factory is successfully started.
     */
    public Observable<Void> start() {
        return Observable.empty();
    }

    /**
     * Shutdown this factory.
     */
    public void shutdown() {
        shutdown = true;
        shutdownHook.onCompleted();
    }

    /**
     * A hook to listen for shutdown of this factory.
     *
     * @return {@code Observable} which completes when this factory shutsdown.
     */
    public final Observable<Void> shutdownHook() {
        return shutdownHook;
    }

    protected boolean isShutdown() {
        return shutdown;
    }

    final <WW, RR> ConnectionProvider<WW, RR> transform(final Func1<ConnectionFactory<WW, RR>, ConnectionFactory<W, R>> func) {
        final Func1<ConnectionFactory<WW, RR>, ConnectionProvider<WW, RR>> cf =
                new Func1<ConnectionFactory<WW, RR>, ConnectionProvider<WW, RR>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public ConnectionProvider<WW, RR> call(ConnectionFactory<WW, RR> connectionFactory) {
                        ConnectionFactory<W, R> oldTypeFactory = func.call(connectionFactory);
                        return (ConnectionProvider<WW, RR>) creationFunc.call(oldTypeFactory);
                    }
                };
        return create(cf);
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
        return forHost(host, port, RxNetty.getRxEventLoopProvider().globalClientEventLoop(), NioSocketChannel.class);
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
        return forHost(new InetSocketAddress(host, port), eventLoopGroup, channelClass);
    }

    /**
     * Creates a concrete {@link ConnectionProvider} for the passed {@code host}
     *
     * @param host The target server address.
     *
     * @return A concrete {@link ConnectionProvider} for the passed {@code host}.
     */
    public static <W, R> ConnectionProvider<W, R> forHost(final SocketAddress host) {
        return forHost(host, RxNetty.getRxEventLoopProvider().globalClientEventLoop(), NioSocketChannel.class);
    }

    /**
     * Creates a concrete {@link ConnectionProvider} for the passed {@code host}
     *
     * @param host The target server address.
     * @param eventLoopGroup Event loop group to use.
     * @param channelClass Class of the channel.
     *
     * @return A concrete {@link ConnectionProvider} for the passed {@code host}.
     */
    public static <W, R> ConnectionProvider<W, R> forHost(final SocketAddress host, EventLoopGroup eventLoopGroup,
                                                         Class<? extends Channel> channelClass) {
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
        }, eventLoopGroup, channelClass);
    }

    /**
     * Creates a raw {@link ConnectionProvider} using the provided function to create actual {@link ConnectionProvider}
     * instance.
     *
     * @param func A function to create a concrete {@link ConnectionProvider} using the passed {@link BootstrapFactory}
     *
     * @return A raw connection factory that will use the passed function to create concrete {@link ConnectionProvider}
     * instance.
     */
    public static <W, R> ConnectionProvider<W, R> create(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> func) {
        return create(func, RxNetty.getRxEventLoopProvider().globalClientEventLoop(), NioSocketChannel.class);
    }

    /**
     * Creates a raw {@link ConnectionProvider} using the provided function to create actual {@link ConnectionProvider}
     * instance.
     *
     * @param func A function to create a concrete {@link ConnectionProvider} using the passed {@link BootstrapFactory}
     * @param eventLoopGroup Event loop group to use.
     * @param channelClass Class of the channel.
     *
     * @return A raw connection factory that will use the passed function to create concrete {@link ConnectionProvider}
     * instance.
     */
    public static <W, R> ConnectionProvider<W, R> create(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> func,
                                                        EventLoopGroup eventLoopGroup,
                                                        Class<? extends Channel> channelClass) {
        return new UninitializedConnectionProvider<W, R>(func, eventLoopGroup, channelClass);
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

    private static class UninitializedCreationFunc<W, R>
            implements Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> {

        @Override
        public ConnectionProvider<W, R> call(ConnectionFactory<W, R> connectionFactory) {
            return new UninitializedConnectionProvider<W, R>(connectionFactory);
        }
    }

    private static class UninitializedConnectionProvider<W, R> extends ConnectionProvider<W, R> {

        private UninitializedConnectionProvider(ConnectionFactory<W, R> connectionFactory) {
            super(connectionFactory);
        }

        private UninitializedConnectionProvider(Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>> func,
                                                EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass) {
            super(func, eventLoopGroup, channelClass);
        }

        @Override
        public ConnectionObservable<R, W> nextConnection() {
            return ConnectionObservable.forError(new IllegalStateException("Connection factory not initialized"));
        }

        @Override
        public Observable<Void> start() {
            return Observable.error(new IllegalStateException("Connection factory not initialized"));
        }
    }
}
