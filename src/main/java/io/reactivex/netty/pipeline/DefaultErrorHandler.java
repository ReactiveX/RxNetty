package io.reactivex.netty.pipeline;

import io.reactivex.netty.server.ErrorHandler;
import rx.Observable;

/**
* @author Nitesh Kant
*/
class DefaultErrorHandler implements ErrorHandler {

    @Override
    public Observable<Void> handleError(Throwable throwable) {
        System.err.println("Unexpected error in RxNetty. Error: " + throwable.getMessage());
        throwable.printStackTrace(System.err);
        return Observable.empty();
    }
}
