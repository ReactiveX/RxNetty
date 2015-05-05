/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.client;

import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.observers.Subscribers;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Nitesh Kant
 */
public class ConnectionPoolImpl<I, O> implements ConnectionPool<I, O> {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolImpl.class);

    @Deprecated
    @SuppressWarnings("unused")
    public static final PoolExhaustedException POOL_EXHAUSTED_EXCEPTION = new PoolExhaustedException("Rx Connection Pool exhausted.");

    private final ConcurrentLinkedQueue<PooledConnection<I, O>> idleConnections;
    private final ClientChannelFactory<I, O> channelFactory;
    private final ClientConnectionFactory<I, O, PooledConnection<I, O>> connectionFactory;
    private final PoolLimitDeterminationStrategy limitDeterminationStrategy;
    private final MetricEventsSubject<ClientMetricsEvent<?>> metricEventsSubject;
    private final RxClient.ServerInfo serverInfo;
    private final PoolConfig poolConfig;
    private final ScheduledExecutorService cleanupScheduler;
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    /*Nullable*/ private final ScheduledFuture<?> idleConnCleanupScheduleFuture;

    /**
     * Creates a new connection pool instance.
     * @param serverInfo Server to which this pool connects.
     * @param poolConfig The pool configuration.
     * @param strategy Pool limit determination strategy. This can be {@code null}
     * @param cleanupScheduler Pool idle cleanup scheduler. This can be {@code null} which means there will be
     * @param eventsSubject Metrics event subject to use.
     */
    public ConnectionPoolImpl(RxClient.ServerInfo serverInfo, PoolConfig poolConfig,
                              PoolLimitDeterminationStrategy strategy, ScheduledExecutorService cleanupScheduler,
                              ClientChannelFactory<I, O> channelFactory,
                              MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this(serverInfo, poolConfig, strategy, cleanupScheduler,
             new PooledConnectionFactory<I, O>(poolConfig, eventsSubject), channelFactory, eventsSubject);
    }

    /**
     * Creates a new connection pool instance.
     *  @param serverInfo Server to which this pool connects.
     * @param poolConfig The pool configuration.
     * @param strategy Pool limit determination strategy. This can be {@code null}
     * @param cleanupScheduler Pool idle cleanup scheduler. This can be {@code null} which means there will be
     * @param eventsSubject Metrics event subject to use.
     */
    public ConnectionPoolImpl(RxClient.ServerInfo serverInfo, PoolConfig poolConfig,
                              PoolLimitDeterminationStrategy strategy, ScheduledExecutorService cleanupScheduler,
                              ClientConnectionFactory<I, O, PooledConnection<I, O>> connectionFactory,
                              ClientChannelFactory<I, O> channelFactory,
                              MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.serverInfo = serverInfo;
        this.poolConfig = poolConfig;
        this.cleanupScheduler = cleanupScheduler;
        this.connectionFactory = connectionFactory;
        this.channelFactory = channelFactory;
        metricEventsSubject = eventsSubject;

        long scheduleDurationMillis = Math.max(30, this.poolConfig.getMaxIdleTimeMillis()); // Ignore too agressive durations as they create a lot of thread spin.

        if (null != cleanupScheduler) {
            idleConnCleanupScheduleFuture = this.cleanupScheduler.scheduleWithFixedDelay(
                    new IdleConnectionsCleanupTask(), scheduleDurationMillis, scheduleDurationMillis, TimeUnit.MILLISECONDS);
        } else {
            idleConnCleanupScheduleFuture = null;
        }

        limitDeterminationStrategy = null == strategy ? new MaxConnectionsBasedStrategy() : strategy;
        metricEventsSubject.subscribe(limitDeterminationStrategy);
        idleConnections = new ConcurrentLinkedQueue<PooledConnection<I, O>>();
    }

    @Override
    public Observable<ObservableConnection<I, O>> acquire() {

        if (isShutdown.get()) {
            return Observable.error(new IllegalStateException("Connection pool is already shutdown."));
        }

        return Observable.create(new Observable.OnSubscribe<ObservableConnection<I, O>>() {
            @Override
            public void call(final Subscriber<? super ObservableConnection<I, O>> subscriber) {
                long startTimeMillis = Clock.newStartTimeMillis();
                try {
                    metricEventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_START);
                    PooledConnection<I, O> idleConnection = getAnIdleConnection(true);

                    if (null != idleConnection) { // Found a usable connection
                        idleConnection.beforeReuse();
                        channelFactory.onNewConnection(idleConnection, subscriber);
                        long endTime = Clock.onEndMillis(startTimeMillis);
                        metricEventsSubject.onEvent(ClientMetricsEvent.POOLED_CONNECTION_REUSE, endTime);
                        metricEventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_SUCCESS, endTime);
                    } else if (limitDeterminationStrategy.acquireCreationPermit(startTimeMillis,
                                                                                TimeUnit.MILLISECONDS)) { // Check if it is allowed to create another connection.
                        /**
                         * Here we want to make sure that if the connection attempt failed, we should inform the strategy.
                         * Failure to do so, will leak the permits from the strategy. So, any code in this block MUST
                         * ALWAYS use this new subscriber instead of the original subscriber to send any callbacks.
                         */
                        Subscriber<? super ObservableConnection<I, O>> newConnectionSubscriber =
                                newConnectionSubscriber(subscriber, startTimeMillis);
                        try {
                            channelFactory.connect(newConnectionSubscriber, serverInfo,
                                                   connectionFactory); // Manages the callbacks to the subscriber
                        } catch (Throwable throwable) {
                            newConnectionSubscriber.onError(throwable);
                        }
                    } else { // Pool Exhausted
                        PoolExhaustedException e = new PoolExhaustedException();
                        metricEventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_FAILED,
                                                    Clock.onEndMillis(startTimeMillis), e);
                        subscriber.onError(e);
                    }
                } catch (Throwable throwable) {
                    metricEventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_FAILED,
                                                Clock.onEndMillis(startTimeMillis), throwable);
                    subscriber.onError(throwable);
                }
            }
        });
    }

    @Override
    public Observable<Void> release(PooledConnection<I, O> connection) {

        // Its legal to release after shutdown as it is not under anyones control when the connection returns. Its
        // usually user initiated.

        if (null == connection) {
            return Observable.error(new IllegalArgumentException("Returned a null connection to the pool."));
        }

        long startTimeMillis = Clock.newStartTimeMillis();

        try {
            connection.getChannel().pipeline().fireUserEventTriggered(new PooledConnectionReleasedEvent(connection));
            metricEventsSubject.onEvent(ClientMetricsEvent.POOL_RELEASE_START);
            if (isShutdown.get() || !connection.isUsable()) {
                discardConnection(connection);
                metricEventsSubject.onEvent(ClientMetricsEvent.POOL_RELEASE_SUCCESS, Clock.onEndMillis(
                        startTimeMillis));
                return Observable.empty();
            } else {
                idleConnections.add(connection);
                metricEventsSubject.onEvent(ClientMetricsEvent.POOL_RELEASE_SUCCESS, Clock.onEndMillis(
                        startTimeMillis));
                return Observable.empty();
            }
        } catch (Throwable throwable) {
            metricEventsSubject.onEvent(ClientMetricsEvent.POOL_RELEASE_FAILED, Clock.onEndMillis(startTimeMillis));
            return Observable.error(throwable);
        }
    }

    @Override
    public Observable<Void> discard(PooledConnection<I, O> connection) {
        // Its legal to discard after shutdown as it is not under anyones control when the connection returns. Its
        // usually initiated by underlying connection close.

        if (null == connection) {
            return Observable.error(new IllegalArgumentException("Returned a null connection to the pool."));
        }

        boolean removed = idleConnections.remove(connection);

        if (removed) {
            discardConnection(connection);
        }

        return Observable.empty();
    }

    @Override
    public void shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            return;
        }

        if (null != idleConnCleanupScheduleFuture) {
            idleConnCleanupScheduleFuture.cancel(true);
        }
        PooledConnection<I, O> idleConnection = getAnIdleConnection(true);
        while (null != idleConnection) {
            discardConnection(idleConnection);
            idleConnection = getAnIdleConnection(true);
        }
        metricEventsSubject.onCompleted();
    }

    private PooledConnection<I, O> getAnIdleConnection(boolean claimConnectionIfFound) {
        PooledConnection<I, O> idleConnection;
        while ((idleConnection = idleConnections.poll()) != null) {
            if (!idleConnection.isUsable()) {
                discardConnection(idleConnection);
            } else if (claimConnectionIfFound) {
                if (idleConnection.claim()) {
                    break;
                }
            } else {
                break;
            }
        }
        return idleConnection;
    }

    private Observable<Void> discardConnection(PooledConnection<I, O> idleConnection) {
        metricEventsSubject.onEvent(ClientMetricsEvent.POOLED_CONNECTION_EVICTION);
        return idleConnection.closeUnderlyingChannel();
    }

    private Subscriber<? super ObservableConnection<I, O>> newConnectionSubscriber(
            final Subscriber<? super ObservableConnection<I, O>> subscriber, final long startTime) {
        return Subscribers.create(new Action1<ObservableConnection<I, O>>() {
                                      @Override
                                      public void call(ObservableConnection<I, O> connection) {
                                          metricEventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_SUCCESS,
                                                                      Clock.onEndMillis(startTime));
                                          PooledConnection<I, O> pooledConnection = (PooledConnection<I, O>) connection;
                                          pooledConnection.setConnectionPool(ConnectionPoolImpl.this);
                                          /**
                                           * Issue: https://github.com/Netflix/RxNetty/issues/183
                                           * Pool configuration can be updated after creating the ClientConnectionFactory
                                           * in ConnectionPoolBuilder. It is pretty convoluted to update the pool config
                                           * in the connection factory as not all connection factories are pooled.
                                           * This does not deserve creating a separate interface for pooled factories.
                                           * Since, max idle timeout in itself is updatable, this is a better route to
                                           * control the final connection as opposed to the factory.
                                           */
                                          pooledConnection.updateMaxIdleTimeMillis(poolConfig.getMaxIdleTimeMillis());
                                          subscriber.onNext(connection);
                                          subscriber.onCompleted(); // This subscriber is for "A" connection, so it should be completed.
                                      }
                                  }, new Action1<Throwable>() {
                                      @Override
                                      public void call(Throwable throwable) {
                                          metricEventsSubject.onEvent(ClientMetricsEvent.POOL_ACQUIRE_FAILED,
                                                                      Clock.onEndMillis(startTime), throwable);
                                          subscriber.onError(throwable);
                                      }
                                  }
        );
    }

    @Override
    public Subscription subscribe(MetricEventsListener<? extends ClientMetricsEvent<?>> listener) {
        return metricEventsSubject.subscribe(listener);
    }

    private class IdleConnectionsCleanupTask implements Runnable {

        @Override
        public void run() {
            try {
                Iterator<PooledConnection<I,O>> iterator = idleConnections.iterator(); // Weakly consistent iterator
                while (iterator.hasNext()) {
                    PooledConnection<I, O> idleConnection = iterator.next();
                    if (!idleConnection.isUsable() && idleConnection.claim()) {
                        iterator.remove();
                        discardConnection(idleConnection); // Don't use pool.discard() as that won't do anything if the
                                                           // connection isn't there in the idle queue, which is the case here.
                    }
                }
            } catch (Exception e) {
                logger.error("Exception in the idle connection cleanup task. This does NOT stop the next schedule of the task. ", e);
            }
        }
    }
}
