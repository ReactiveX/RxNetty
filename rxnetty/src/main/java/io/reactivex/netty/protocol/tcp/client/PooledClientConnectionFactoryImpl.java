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

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.PoolConfig;
import io.reactivex.netty.client.PoolExhaustedException;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.client.ClientMetricsEvent.*;

/**
 * An implementation of {@link ClientConnectionFactory} that pools connections. Configuration of the pool is as defined
 * by {@link PoolConfig} passed in with the {@link ClientState}.
 *
 * Following are the key parameters:
 *
 * <ul>
 <li>{@link PoolLimitDeterminationStrategy}: A stratgey to determine whether a new physical connection should be
 created as part of the user request.</li>
 <li>{@link PoolConfig#getIdleConnectionsCleanupTimer()}: The schedule for cleaning up idle connections in the pool.</li>
 <li>{@link PoolConfig#getMaxIdleTimeMillis()}: Maximum time a connection can be idle in this pool.</li>
 </ul>
 *
 * @param <W> Type of object that is written to the client using this factory.
 * @param <R> Type of object that is read from the the client using this factory.
 *
 * @author Nitesh Kant
 */
public class PooledClientConnectionFactoryImpl<W, R> extends PooledClientConnectionFactory<W, R> {

    private static final Logger logger = LoggerFactory.getLogger(PooledClientConnectionFactoryImpl.class);

    private final ClientConnectionFactory<W, R> connectDelegate;
    private final Subscription idleConnCleanupSubscription;
    private final MetricEventsSubject<ClientMetricsEvent<?>> metricsEventSubject;
    private final IdleConnectionsHolder<W, R> idleConnectionsHolder;

    private final Observable<PooledConnection<R, W>> idleConnFinderObservable;
    private final PoolLimitDeterminationStrategy limitDeterminationStrategy;

    public PooledClientConnectionFactoryImpl(ClientState<W, R> clientState) {
        this(clientState, new FIFOIdleConnectionsHolder<W, R>(),
             new UnpooledClientConnectionFactory<W, R>(clientState));
    }

    protected PooledClientConnectionFactoryImpl(ClientState<W, R> clientState,
                                             IdleConnectionsHolder<W, R> connectionsHolder) {
        this(clientState, connectionsHolder, new UnpooledClientConnectionFactory<W, R>(clientState));
    }

    protected PooledClientConnectionFactoryImpl(ClientState<W, R> clientState,
                                                IdleConnectionsHolder<W, R> connectionsHolder,
                                                ClientConnectionFactory<W, R> delegate) {
        super(clientState.getPoolConfig(), clientState);
        connectDelegate = delegate;
        idleConnectionsHolder = connectionsHolder;
        idleConnFinderObservable = idleConnectionsHolder.pollThisEventLoopConnections()
                                                        .doOnSubscribe(new Action0() {
                                                            @Override
                                                            public void call() {
                                                                metricsEventSubject.onEvent(POOL_ACQUIRE_START);
                                                            }
                                                        })
                                                        .concatWith(idleConnectionsHolder.poll())
                                                        .lift(new SingleConnectionFinder());
        metricsEventSubject = clientState.getEventsSubject();
        limitDeterminationStrategy = clientState.getPoolConfig().getPoolLimitDeterminationStrategy();
        // In case, there is no cleanup required, this observable should never give a tick.
        idleConnCleanupSubscription = poolConfig.getIdleConnectionsCleanupTimer()
                                                .doOnError(LogErrorAction.INSTANCE)
                                                .retry() // Retry when there is an error in timer.
                                                .concatMap(new IdleConnectionCleanupTask())
                                                .onErrorResumeNext(new Func1<Throwable, Observable<Void>>() {
                                                    @Override
                                                    public Observable<Void> call(Throwable throwable) {
                                                        logger.error("Ignoring error cleaning up idle connections.",
                                                                     throwable);
                                                        return Observable.empty();
                                                    }
                                                }) // Ignore errors in cleanup.
                                                .subscribe(Actions.empty()); // Errors are logged and ignored.
    }

    @Override
    public Observable<? extends Connection<R, W>> connect() {
        if (isShutdown()) {
            return Observable.error(new IllegalStateException("Connection factory is already shutdown."));
        }

        return idleConnFinderObservable.switchIfEmpty(connectIfAllowed());
    }

    @Override
    public Observable<Void> release(final PooledConnection<R, W> connection) {

        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                if (null == connection) {
                    subscriber.onCompleted();
                } else {
                    /**
                     * Executing the release on the eventloop to avoid race-conditions between code cleaning up
                     * connection in the pipeline and the connecting being released to the pool.
                     */
                    connection.getNettyChannel()
                              .eventLoop()
                              .submit(new ReleaseTask(connection, subscriber));
                }
            }
        });
    }

    @Override
    public Observable<Void> discard(PooledConnection<R, W> connection) {
        return idleConnectionsHolder.discard(connection);
    }

    @Override
    public void shutdown() {
        idleConnCleanupSubscription.unsubscribe();
        connectDelegate.shutdown();
    }

    @Override
    protected <WW, RR> ClientConnectionFactory<WW, RR> doCopy(ClientState<WW, RR> newState) {
        return new PooledClientConnectionFactoryImpl<WW, RR>(newState, idleConnectionsHolder.copy(newState),
                                                             connectDelegate.copy(newState));
    }

    private Observable<PooledConnection<R, W>> connectIfAllowed() {
        return Observable.create(new OnSubscribe<PooledConnection<R, W>>() {
            @Override
            public void call(Subscriber<? super PooledConnection<R, W>> subscriber) {
                final long startTimeMillis = Clock.newStartTimeMillis();
                if (limitDeterminationStrategy.acquireCreationPermit(startTimeMillis, TimeUnit.MILLISECONDS)) {
                    connectDelegate.connect()
                                   .map(new Func1<Connection<R, W>, PooledConnection<R, W>>() {
                                       @Override
                                       public PooledConnection<R, W> call(Connection<R, W> connection) {
                                           metricsEventSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_SUCCESS,
                                                                       Clock.onEndMillis(startTimeMillis));
                                           return PooledConnection.create(PooledClientConnectionFactoryImpl.this,
                                                                          poolConfig, connection);
                                       }
                                   })
                                   .doOnError(new Action1<Throwable>() {
                                       @Override
                                       public void call(Throwable throwable) {
                                           metricsEventSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_FAILED,
                                                                       Clock.onEndMillis(startTimeMillis), throwable);
                                       }
                                   })
                                   .subscribe(subscriber);
                } else {
                    subscriber.onError(new PoolExhaustedException("Client connection pool exhausted."));
                }
            }
        });
    }

    private static class LogErrorAction implements Action1<Throwable> {

        public static final LogErrorAction INSTANCE = new LogErrorAction();

        @Override
        public void call(Throwable throwable) {
            logger.error("Error from idle connection cleanup timer. This will be retried.", throwable);
        }
    }

    private class IdleConnectionCleanupTask implements Func1<Long, Observable<Void>> {

        @Override
        public Observable<Void> call(Long aLong) {
            return idleConnectionsHolder.peek()
                                        .map(new Func1<PooledConnection<R, W>, Void>() {
                                            @Override
                                            public Void call(PooledConnection<R, W> connection) {
                                                if (!connection.isUsable()) {
                                                    idleConnectionsHolder.remove(connection);
                                                    discard(connection)
                                                        .subscribe(Actions.empty(),
                                                                   new Action1<Throwable>() {
                                                                       @Override
                                                                       public void call(Throwable throwable) {
                                                                           logger.error("Failed to discard connection.",
                                                                                        throwable);
                                                                       }
                                                                   });
                                                }
                                                return null;
                                            }
                                        }).ignoreElements();
        }
    }

    private class ReleaseTask implements Runnable {

        private final PooledConnection<R, W> connection;
        private final Subscriber<? super Void> subscriber;
        private final long releaseStartTime;

        private ReleaseTask(PooledConnection<R, W> connection, Subscriber<? super Void> subscriber) {
            this.connection = connection;
            this.subscriber = subscriber;
            releaseStartTime = Clock.newStartTimeMillis();
        }

        @Override
        public void run() {
            try {
                metricsEventSubject.onEvent(POOL_RELEASE_START);
                if (isShutdown() || !connection.isUsable()) {
                    idleConnectionsHolder.discard(connection);
                    metricsEventSubject.onEvent(POOL_RELEASE_SUCCESS, Clock.onEndMillis(releaseStartTime));
                } else {
                    idleConnectionsHolder.add(connection);
                    metricsEventSubject.onEvent(POOL_RELEASE_SUCCESS, Clock.onEndMillis(releaseStartTime));
                }
                subscriber.onCompleted();
            } catch (Throwable throwable) {
                metricsEventSubject.onEvent(POOL_RELEASE_FAILED, Clock.onEndMillis(releaseStartTime));
            }
        }
    }

    /**
     * Why isn't this a filter and map?
     * The only reason for existence of this operator is to record the time taken to fetch an idle connection from the
     * pool. The start time must be the subscription time, which isn't possible to get via a map function.
     */
    private class SingleConnectionFinder implements Operator<PooledConnection<R, W>, PooledConnection<R, W>> {

        @Override
        public Subscriber<? super PooledConnection<R, W>> call(final Subscriber<? super PooledConnection<R, W>> subscriber) {

            final long startTimeMillis = Clock.newStartTimeMillis();

            return new Subscriber<PooledConnection<R, W>>(subscriber) {

                private volatile boolean foundConnection;

                @Override
                public void onCompleted() {
                    if (foundConnection) {
                        metricsEventSubject.onEvent(POOL_ACQUIRE_SUCCESS, Clock.onEndMillis(startTimeMillis));
                    }

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    metricsEventSubject.onEvent(POOL_ACQUIRE_FAILED, Clock.onEndMillis(startTimeMillis), e);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }

                @Override
                public void onNext(PooledConnection<R, W> connection) {
                    if (connection.isUsable()) {
                        foundConnection = true;
                        metricsEventSubject.onEvent(POOLED_CONNECTION_REUSE, Clock.onEndMillis(startTimeMillis));
                        connection.reuse(subscriber);
                    } else {
                        idleConnectionsHolder.discard(connection);
                    }
                }
            };
        }
    }
}