package io.reactivex.netty.client;

/**
 * A contract for statistics maintained by the {@link ConnectionPool}
 *
 * @author Nitesh Kant
 */
public interface PoolStats {

    long getInUseCount();

    long getIdleCount();

    long getTotalConnectionCount();

    long getPendingAcquireRequestCount();

    long getPendingReleaseRequestCount();
}
