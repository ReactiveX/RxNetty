package io.reactivex.netty.client;

import com.netflix.numerus.LongAdder;

/**
* @author Nitesh Kant
*/
public class TrackableStateChangeListener implements PoolStateChangeListener {

    private final LongAdder creationCount = new LongAdder();
    private final LongAdder failedCount = new LongAdder();
    private final LongAdder reuseCount = new LongAdder();
    private final LongAdder evictionCount = new LongAdder();
    private final LongAdder acquireAttemptedCount = new LongAdder();
    private final LongAdder acquireSucceededCount = new LongAdder();
    private final LongAdder acquireFailedCount = new LongAdder();
    private final LongAdder releaseAttemptedCount = new LongAdder();
    private final LongAdder releaseSucceededCount = new LongAdder();
    private final LongAdder releaseFailedCount = new LongAdder();

    @Override
    public void onConnectionCreation() {
        creationCount.increment();
    }

    @Override
    public void onConnectFailed() {
        failedCount.increment();
    }

    @Override
    public void onConnectionReuse() {
        reuseCount.increment();
    }

    @Override
    public void onConnectionEviction() {
        evictionCount.increment();
    }

    @Override
    public void onAcquireAttempted() {
        acquireAttemptedCount.increment();
    }

    @Override
    public void onAcquireSucceeded() {
        acquireSucceededCount.increment();
    }

    @Override
    public void onAcquireFailed() {
        acquireFailedCount.increment();
    }

    @Override
    public void onReleaseAttempted() {
        releaseAttemptedCount.increment();
    }

    @Override
    public void onReleaseSucceeded() {
        releaseSucceededCount.increment();
    }

    @Override
    public void onReleaseFailed() {
        releaseFailedCount.increment();
    }

    public long getAcquireAttemptedCount() {
        return acquireAttemptedCount.longValue();
    }

    public long getAcquireFailedCount() {
        return acquireFailedCount.longValue();
    }

    public long getAcquireSucceededCount() {
        return acquireSucceededCount.longValue();
    }

    public long getCreationCount() {
        return creationCount.longValue();
    }

    public long getEvictionCount() {
        return evictionCount.longValue();
    }

    public long getFailedCount() {
        return failedCount.longValue();
    }

    public long getReleaseAttemptedCount() {
        return releaseAttemptedCount.longValue();
    }

    public long getReleaseFailedCount() {
        return releaseFailedCount.longValue();
    }

    public long getReleaseSucceededCount() {
        return releaseSucceededCount.longValue();
    }

    public long getReuseCount() {
        return reuseCount.longValue();
    }
}
