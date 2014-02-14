package io.reactivex.netty.protocol.http.client;

import rx.Observer;

/**
 * Delegates the {@link #onCompleted()} and {@link #onError(Throwable)} to all contained observers.
 *
 * @author Nitesh Kant
 */
public abstract class CompositeObserver<T> implements Observer<T> {

    @SuppressWarnings("rawtypes") private final Observer[] underlyingObservers;

    protected CompositeObserver(@SuppressWarnings("rawtypes") Observer... underlyingObservers) {
        this.underlyingObservers = underlyingObservers;
    }

    @Override
    public void onCompleted() {
        for (@SuppressWarnings("rawtypes") Observer underlyingObserver : underlyingObservers) {
            underlyingObserver.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        for (@SuppressWarnings("rawtypes") Observer underlyingObserver : underlyingObservers) {
            underlyingObserver.onError(e);
        }
    }
}
