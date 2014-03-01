package io.reactivex.netty.protocol.http.server;

/**
 * @author Nitesh Kant
 */
public interface ErrorResponseGenerator<T> {

    void updateResponse(HttpResponse<T> response, Throwable error);
}
