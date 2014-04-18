package io.reactivex.netty.channel;

import rx.Observer;

/**
 * Fired whenever a new {@link ObservableConnection} instance is created.
 *
 * @author Nitesh Kant
 */
public class NewRxConnectionEvent {

    @SuppressWarnings("rawtypes") private final Observer connectedObserver;

    public NewRxConnectionEvent(@SuppressWarnings("rawtypes") Observer connectedObserver) {
        this.connectedObserver = connectedObserver;
    }

    public @SuppressWarnings("rawtypes") Observer getConnectedObserver() {
        return connectedObserver;
    }
}
