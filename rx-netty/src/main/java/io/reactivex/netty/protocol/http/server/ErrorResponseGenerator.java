package io.reactivex.netty.protocol.http.server;

/**
 * @author Nitesh Kant
 */
public interface ErrorResponseGenerator<T> {

    void updateResponse(HttpServerResponse<T> response, Throwable error);
}
