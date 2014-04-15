package io.reactivex.netty.client;

import com.netflix.numerus.LongAdder;

/**
 * @author Nitesh Kant
 */
public class PoolStatsImpl implements PoolStats, PoolStateChangeListener {

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
    public void onConnectionCreation() {
        totalConnections.increment();
    }

    @Override
    public void onConnectFailed() {
        // No op
    }

    @Override
    public void onConnectionReuse() {
        idleConnections.decrement();
    }

    @Override
    public void onConnectionEviction() {
        idleConnections.decrement();
        totalConnections.decrement();
    }

    @Override
    public void onAcquireAttempted() {
        pendingAcquires.increment();
    }

    @Override
    public void onAcquireSucceeded() {
        inUseConnections.increment();
        pendingAcquires.decrement();
    }

    @Override
    public void onAcquireFailed() {
        pendingAcquires.decrement();
    }

    @Override
    public void onReleaseAttempted() {
        pendingReleases.increment();
    }

    @Override
    public void onReleaseSucceeded() {
        idleConnections.increment();
        inUseConnections.decrement();
        pendingReleases.decrement();
    }

    @Override
    public void onReleaseFailed() {
        pendingReleases.decrement();
    }
}
