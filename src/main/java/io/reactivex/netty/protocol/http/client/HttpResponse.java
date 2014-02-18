/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http.client;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.CookiesHolder;
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
    private final CookiesHolder cookiesHolder;

    public HttpResponse(io.netty.handler.codec.http.HttpResponse nettyResponse, PublishSubject<T> contentSubject) {
        this.nettyResponse = nettyResponse;
        this.contentSubject = contentSubject;
        httpVersion = this.nettyResponse.getProtocolVersion();
        status = this.nettyResponse.getStatus();
        responseHeaders = new HttpResponseHeaders(nettyResponse);
        cookiesHolder = CookiesHolder.newClientResponseHolder(nettyResponse.headers());
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
        return cookiesHolder.getAllCookies();
    }

    public Observable<T> getContent() {
        return contentSubject;
    }

    public <R> Observable<R> getContent(@SuppressWarnings("unused") Func1<T, R> somestuff) {
        // TODO: SerDe Framework
        return Observable.error(new UnsupportedOperationException("On-demand de-serialization is not supported."));
    }
}
