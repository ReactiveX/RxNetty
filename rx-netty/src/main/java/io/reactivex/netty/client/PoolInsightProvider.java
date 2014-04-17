package io.reactivex.netty.client;

import rx.Observable;

/**
 * An interface providing insights into the connection pool. This essentially separates read from write operations of a
 * {@link ConnectionPool}
 *
 * @author Nitesh Kant
 */
public interface PoolInsightProvider {

    /**
     * Returns the {@link Observable} that emits any changes to the state of the pool as {@link StateChangeEvent}
     *
     * @return An {@link Observable} emitting all state change events to the pool.
     */
    Observable<StateChangeEvent> stateChangeObservable();

    PoolStats getStats();

    enum StateChangeEvent {
        NewConnectionCreated,
        ConnectFailed,
        OnConnectionReuse,
        OnConnectionEviction,
        onAcquireAttempted,
        onAcquireSucceeded,
        onAcquireFailed,
        onReleaseAttempted,
        onReleaseSucceeded,
        onReleaseFailed
    }
}
