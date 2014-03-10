package io.reactivex.netty.protocol.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
* @author Nitesh Kant
*/
class DefaultErrorResponseGenerator<O> implements ErrorResponseGenerator<O> {
    @Override
    public void updateResponse(HttpServerResponse<O> response, Throwable error) {
        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
}
