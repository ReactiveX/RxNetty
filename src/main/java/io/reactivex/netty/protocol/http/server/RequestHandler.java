package io.reactivex.netty.protocol.http.server;

import rx.Observable;

/**
 * @author Nitesh Kant
 */
public interface RequestHandler<I, O> {

    Observable<Void> handle(HttpRequest<I> request, HttpResponse<O> response);

}
