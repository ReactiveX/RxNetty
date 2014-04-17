package io.reactivex.netty.client;

import com.netflix.numerus.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observer;

/**
 * @author Nitesh Kant
 */
public class PoolStatsImpl implements PoolStats, PoolStatsProvider {

    private static final Logger logger = LoggerFactory.getLogger(PoolStatsImpl.class);

    private final LongAdder idleConnections;
    private final LongAdder inUseConnections;
    private final LongAdder totalConnections;
    private final LongAdder pendingAcquires;
    private final LongAdder pendingReleases;

    public PoolStatsImpl() {
        idleConnections = new LongAdder();
        inUseConnections = new LongAdder();
        totalConnections = new LongAdder();
        pendingAcquires = new LongAdder();
        pendingReleases = new LongAdder();
    }

    @Override
    public long getIdleCount() {
        return idleConnections.longValue();
    }

    @Override
    public long getInUseCount() {
        return inUseConnections.longValue();
    }

    @Override
    public long getTotalConnectionCount() {
        return totalConnections.longValue();
    }

    @Override
    public long getPendingAcquireRequestCount() {
        return pendingAcquires.longValue();
    }

    @Override
    public long getPendingReleaseRequestCount() {
        return pendingReleases.longValue();
    }

    @Override
    public void onCompleted() {
        // No op.
    }

    @Override
    public void onError(Throwable e) {
        logger.error("Connection pool emitted an error for state change events.", e);
    }

    @Override
    public void onNext(PoolInsightProvider.StateChangeEvent stateChangeEvent) {
        switch (stateChangeEvent) {
            case NewConnectionCreated:
                onConnectionCreation();
                break;
            case ConnectFailed:
                onConnectFailed();
                break;
            case OnConnectionReuse:
                onConnectionReuse();
                break;
            case OnConnectionEviction:
                onConnectionEviction();
                break;
            case onAcquireAttempted:
                onAcquireAttempted();
                break;
            case onAcquireSucceeded:
                onAcquireSucceeded();
                break;
            case onAcquireFailed:
                onAcquireFailed();
                break;
            case onReleaseAttempted:
                onReleaseAttempted();
                break;
            case onReleaseSucceeded:
                onReleaseSucceeded();
                break;
            case onReleaseFailed:
                onReleaseFailed();
                break;
        }
    }

    @Override
    public PoolStats getStats() {
        return this;
    }

    private void onConnectionCreation() {
        totalConnections.increment();
    }

    private void onConnectFailed() {
        // No op
    }

    private void onConnectionReuse() {
        idleConnections.decrement();
    }

    private void onConnectionEviction() {
        idleConnections.decrement();
        totalConnections.decrement();
    }

    private void onAcquireAttempted() {
        pendingAcquires.increment();
    }

    private void onAcquireSucceeded() {
        inUseConnections.increment();
        pendingAcquires.decrement();
    }

    private void onAcquireFailed() {
        pendingAcquires.decrement();
    }

    private void onReleaseAttempted() {
        pendingReleases.increment();
    }

    private void onReleaseSucceeded() {
        idleConnections.increment();
        inUseConnections.decrement();
        pendingReleases.decrement();
    }

    private void onReleaseFailed() {
        pendingReleases.decrement();
    }
}
