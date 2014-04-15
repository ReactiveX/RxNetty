package io.reactivex.netty.client;

/**
 * A listener for all events happening on a {@link ConnectionPool}
 *
 * @author Nitesh Kant
 */
public interface PoolStateChangeListener {

    void onConnectionCreation();

    void onConnectFailed();

    void onConnectionReuse();

    void onConnectionEviction();

    void onAcquireAttempted();

    void onAcquireSucceeded();

    void onAcquireFailed();

    void onReleaseAttempted();

    void onReleaseSucceeded();

    void onReleaseFailed();
}
