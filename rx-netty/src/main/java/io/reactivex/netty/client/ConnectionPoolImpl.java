package io.reactivex.netty.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.Subscribers;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Nitesh Kant
 */
class ConnectionPoolImpl<I, O> implements ConnectionPool<I, O> {

    private static final PoolExhaustedException POOL_EXHAUSTED_EXCEPTION = new PoolExhaustedException("Rx Connection Pool exhausted.");

    private final PoolStatsImpl stats;
    private final ConcurrentLinkedQueue<PooledConnection<I, O>> idleConnections;
    private ClientChannelFactory<I, O> channelFactory;
    private final PipelineConfigurator<I, O> pipelineConfigurator;
    private final PoolLimitDeterminationStrategy limitDeterminationStrategy;
    private final PoolStateChangeListener stateChangeListener;
    private final PoolConfig poolConfig;
    private volatile boolean isShutdown;

    ConnectionPoolImpl(PoolConfig poolConfig, PipelineConfigurator<I, O> pipelineConfigurator,
                       PoolStateChangeListener stateChangeListener, PoolLimitDeterminationStrategy strategy) {
        this.poolConfig = poolConfig;
        this.pipelineConfigurator = pipelineConfigurator;
        stats = new PoolStatsImpl();
        limitDeterminationStrategy = null == strategy ? new MaxConnectionsBasedStrategy() : strategy;
        this.stateChangeListener = null == stateChangeListener
                                   ? new CompositePoolStateChangeListener(stats, limitDeterminationStrategy)
                                   : new CompositePoolStateChangeListener(stats, limitDeterminationStrategy, stateChangeListener);
        idleConnections = new ConcurrentLinkedQueue<PooledConnection<I, O>>();
        channelFactory = new NoOpClientChannelFactory<I, O>();
    }

    void setChannelFactory(ClientChannelFactory<I,O> channelFactory) { // There is a transitive circular dep b/w pool & factory hence we have to set the factory later.
        this.channelFactory = channelFactory;
    }

    @Override
    public Observable<ObservableConnection<I, O>> acquire() {

        if (isShutdown) {
            return Observable.error(new IllegalStateException("Connection pool is already shutdown."));
        }

        return Observable.create(new Observable.OnSubscribe<ObservableConnection<I, O>>() {
            @Override
            public void call(final Subscriber<? super ObservableConnection<I, O>> subscriber) {
                try {
                    stateChangeListener.onAcquireAttempted();
                    PooledConnection<I, O> idleConnection = getAnIdleConnection();

                    if (null != idleConnection) { // Found a usable connection
                        idleConnection.beforeReuse();
                        stateChangeListener.onConnectionReuse();
                        stateChangeListener.onAcquireSucceeded();
                        subscriber.onNext(idleConnection);
                        subscriber.onCompleted();
                    } else if (limitDeterminationStrategy
                            .acquireCreationPermit()) { // Check if it is allowed to create another connection.
                        /**
                         * Here we want to make sure that if the connection attempt failed, we should inform the strategy.
                         * Failure to do so, will leak the permits from the strategy. So, any code in this block MUST
                         * ALWAYS use this new subscriber instead of the original subscriber to send any callbacks.
                         */
                        Subscriber<ObservableConnection<I, O>> newConnectionSubscriber = newConnectionSubscriber(
                                subscriber);
                        try {
                            channelFactory.connect(newConnectionSubscriber,
                                                   pipelineConfigurator); // Manages the callbacks to the subscriber
                        } catch (Throwable throwable) {
                            newConnectionSubscriber.onError(throwable);
                        }
                    } else { // Pool Exhausted
                        stateChangeListener.onAcquireFailed();
                        subscriber.onError(POOL_EXHAUSTED_EXCEPTION);
                    }
                } catch (Throwable throwable) {
                    stateChangeListener.onAcquireFailed();
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
        try {
            stateChangeListener.onReleaseAttempted();
            if (isShutdown) {
                discardConnection(connection);
                stateChangeListener.onReleaseSucceeded();
                return Observable.empty();
            } else if (connection.isUsable()) {
                idleConnections.add(connection);
                stateChangeListener.onReleaseSucceeded();
                return Observable.empty();
            } else {
                discardConnection(connection);
                stateChangeListener.onReleaseSucceeded();
                return Observable.empty();
            }
        } catch (Throwable throwable) {
            stateChangeListener.onReleaseFailed();
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
    public PoolStats getStats() {
        return stats;
    }

    @Override
    public void shutdown() {
        if (isShutdown) {
            return;
        }

        isShutdown = true;
        PooledConnection<I, O> idleConnection = getAnIdleConnection();
        while (null != idleConnection) {
            discardConnection(idleConnection);
            idleConnection = getAnIdleConnection();
        }
    }

    @Override
    public ObservableConnection<I, O> newConnection(ChannelHandlerContext ctx) {
        return new PooledConnection<I, O>(ctx, this, poolConfig.getMaxIdleTimeMillis());
    }

    private PooledConnection<I, O> getAnIdleConnection() {
        PooledConnection<I, O> idleConnection = idleConnections.poll();
        while (null != idleConnection) {
            if (!idleConnection.isUsable()) {
                discardConnection(idleConnection);
                idleConnection = idleConnections.poll();
            } else {
                break;
            }
        }
        return idleConnection;
    }

    private Observable<Void> discardConnection(PooledConnection<I, O> idleConnection) {
        stateChangeListener.onConnectionEviction();
        return idleConnection.closeUnderlyingChannel();
    }

    private Subscriber<ObservableConnection<I, O>> newConnectionSubscriber(
            final Subscriber<? super ObservableConnection<I, O>> subscriber) {
        return Subscribers.create(new Action1<ObservableConnection<I, O>>() {
                                      @Override
                                      public void call(ObservableConnection<I, O> o) {
                                          stateChangeListener.onConnectionCreation();
                                          stateChangeListener.onAcquireSucceeded();
                                          subscriber.onNext(o);
                                          subscriber.onCompleted(); // This subscriber is for "A" connection, so it should be completed.
                                      }
                                  }, new Action1<Throwable>() {
                                      @Override
                                      public void call(Throwable throwable) {
                                          stateChangeListener.onConnectFailed();
                                          subscriber.onError(throwable);
                                      }
                                  }
        );
    }

    private static class NoOpClientChannelFactory<I, O> implements ClientChannelFactory<I, O> {

        @Override
        public ChannelFuture connect(Subscriber<? super ObservableConnection<I, O>> subscriber,
                                     PipelineConfigurator<I, O> pipelineConfigurator) {
            throw new IllegalStateException("Client channel factory not set.");
        }
    }
}
