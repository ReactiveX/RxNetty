package io.reactivex.netty.client;

/**
 * @author Nitesh Kant
 */
public class CompositePoolStateChangeListener implements PoolStateChangeListener {

    private final PoolStateChangeListener[] listeners;

    public CompositePoolStateChangeListener(PoolStateChangeListener... listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onConnectionCreation() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onConnectionCreation();
        }
    }

    @Override
    public void onConnectFailed() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onConnectFailed();
        }
    }

    @Override
    public void onConnectionReuse() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onConnectionReuse();
        }
    }

    @Override
    public void onConnectionEviction() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onConnectionEviction();
        }
    }

    @Override
    public void onAcquireAttempted() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onAcquireAttempted();
        }
    }

    @Override
    public void onAcquireSucceeded() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onAcquireSucceeded();
        }
    }

    @Override
    public void onAcquireFailed() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onAcquireFailed();
        }
    }

    @Override
    public void onReleaseAttempted() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onReleaseAttempted();
        }
    }

    @Override
    public void onReleaseSucceeded() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onReleaseSucceeded();
        }
    }

    @Override
    public void onReleaseFailed() {
        for (PoolStateChangeListener listener : listeners) {
            listener.onReleaseFailed();
        }
    }
}
