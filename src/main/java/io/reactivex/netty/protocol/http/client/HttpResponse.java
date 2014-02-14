package io.reactivex.netty.protocol.http.client;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.util.functions.Func1;

import java.util.Map;
import java.util.Set;

/**
 * A Http response object used by {@link HttpClient}
 *
 * @param <T> The type of the default response content. This can be converted to a user defined entity by using
 * {@link #getContent(Func1)}
 *
 * @author Nitesh Kant
 */
public class HttpResponse<T> {

    private final io.netty.handler.codec.http.HttpResponse nettyResponse;
    private final PublishSubject<T> contentSubject;
    private final HttpResponseHeaders responseHeaders;
    private final HttpVersion httpVersion;
    private final HttpResponseStatus status;

    public HttpResponse(io.netty.handler.codec.http.HttpResponse nettyResponse, PublishSubject<T> contentSubject) {
        this.nettyResponse = nettyResponse;
        this.contentSubject = contentSubject;
        httpVersion = this.nettyResponse.getProtocolVersion();
        status = this.nettyResponse.getStatus();
        responseHeaders = new HttpResponseHeaders(nettyResponse);
    }

    public HttpResponseHeaders getHeaders() {
        return responseHeaders;
    }

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public Map<String, Set<Cookie>> getCookies() {
        //TODO: Cookie handling.
        return null;
    }

    public Observable<T> getContent() {
        return contentSubject;
    }

    public <R> Observable<R> getContent(@SuppressWarnings("unused") Func1<T, R> somestuff) {
        // TODO: SerDe Framework
        return null;
    }
}
