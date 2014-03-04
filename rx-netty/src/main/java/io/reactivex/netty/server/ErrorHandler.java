package io.reactivex.netty.server;

import rx.Observable;

/**
 * @author Nitesh Kant
 */
public interface ErrorHandler {

    Observable<Void> handleError(Throwable throwable);

}
