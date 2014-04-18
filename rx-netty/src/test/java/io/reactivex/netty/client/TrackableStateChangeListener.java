package io.reactivex.netty.client;

import com.netflix.numerus.LongAdder;
import rx.Observer;

/**
* @author Nitesh Kant
*/
public class TrackableStateChangeListener implements Observer<PoolInsightProvider.PoolStateChangeEvent> {

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

    public void onConnectionCreation() {
        creationCount.increment();
    }

    public void onConnectFailed() {
        failedCount.increment();
    }

    public void onConnectionReuse() {
        reuseCount.increment();
    }

    public void onConnectionEviction() {
        evictionCount.increment();
    }

    public void onAcquireAttempted() {
        acquireAttemptedCount.increment();
    }

    public void onAcquireSucceeded() {
        acquireSucceededCount.increment();
    }

    public void onAcquireFailed() {
        acquireFailedCount.increment();
    }

    public void onReleaseAttempted() {
        releaseAttemptedCount.increment();
    }

    public void onReleaseSucceeded() {
        releaseSucceededCount.increment();
    }

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

    @Override
    public void onCompleted() {
        // No op
    }

    @Override
    public void onError(Throwable e) {
        // No op
    }

    @Override
    public void onNext(PoolInsightProvider.PoolStateChangeEvent stateChangeEvent) {
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
}
