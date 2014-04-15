package io.reactivex.netty.client;

import rx.Observer;

/**
 * @author Nitesh Kant
 */
public class ConnectionReuseEvent {

    @SuppressWarnings("rawtypes") private final Observer connectedObserver;

    public ConnectionReuseEvent(@SuppressWarnings("rawtypes") Observer connectedObserver) {
        this.connectedObserver = connectedObserver;
    }

    public @SuppressWarnings("rawtypes") Observer getConnectedObserver() {
        return connectedObserver;
    }
}
