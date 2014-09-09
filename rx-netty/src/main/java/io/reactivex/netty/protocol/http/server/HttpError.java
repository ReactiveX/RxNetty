package io.reactivex.netty.protocol.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Encapsulate an exception with a specific HTTP response code
 * so a proper HTTP error response may be generated.
 * 
 * @author elandau
 *
 */
public class HttpError extends Exception {
    private final HttpResponseStatus status;
    
    public HttpError(HttpResponseStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpError(HttpResponseStatus status, String message, Throwable t) {
        super(message, t);
        this.status = status;
    }
    public HttpError(HttpResponseStatus status, Throwable t) {
        super(t);
        this.status = status;
    }
    public HttpError(HttpResponseStatus status) {
        this.status = status;
    }
    public HttpResponseStatus getStatus() {
        return status;
    }
    
    public String toString() {
        return "" + status.toString() + " : " + super.toString();
    }
}
