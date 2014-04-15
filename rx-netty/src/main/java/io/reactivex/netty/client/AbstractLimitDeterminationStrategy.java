package io.reactivex.netty.client;

/**
 * An implementation of {@link PoolLimitDeterminationStrategy} that implements all method of
 * {@link PoolStateChangeListener} as a no-op. So, any strategy implementation can override the callbacks they are
 * interested.
 *
 * @author Nitesh Kant
 */
public abstract class AbstractLimitDeterminationStrategy implements PoolLimitDeterminationStrategy {

    @Override
    public void onConnectionCreation() {
    }

    @Override
    public void onConnectFailed() {
    }

    @Override
    public void onConnectionReuse() {
    }

    @Override
    public void onConnectionEviction() {
    }

    @Override
    public void onAcquireAttempted() {
    }

    @Override
    public void onAcquireSucceeded() {
    }

    @Override
    public void onAcquireFailed() {
    }

    @Override
    public void onReleaseAttempted() {
    }

    @Override
    public void onReleaseSucceeded() {
    }

    @Override
    public void onReleaseFailed() {
    }
}
