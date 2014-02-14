package io.reactivex.netty;

import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.server.RxServer;
import rx.Observable;

/**
 * A connection handler invoked for every new connection is established by {@link RxServer} or {@link RxClient}
 *
 * @param <I> The type of the object that is read from a new connection.
 * @param <O> The type of objects that are written to a new connection.
 *
 * @author Nitesh Kant
 */
public interface ConnectionHandler<I, O> {

    /**
     * Invoked whenever a new connection is established.
     *
     * @param newConnection Newly established connection.
     *
     * @return An {@link Observable}, unsubscribe from which should cancel the handling.
     */
    Observable<Void> handle(ObservableConnection<I, O> newConnection);
}
