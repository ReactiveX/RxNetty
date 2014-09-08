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

package io.reactivex.netty.protocol.http.server;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.CookiesHolder;
import rx.Observable;
import rx.subjects.Subject;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nitesh Kant
 */
public class HttpServerRequest<T> {

    private final HttpRequest nettyRequest;
    private final HttpRequestHeaders headers;
    private final Subject<T, T> contentSubject;
    private final HttpMethod method;
    private final HttpVersion protocolVersion;
    private final UriInfoHolder uriInfoHolder;
    private final CookiesHolder cookiesHolder;

    public HttpServerRequest(HttpRequest nettyRequest, Subject<T, T> contentSubject) {
        this.nettyRequest = nettyRequest;
        headers = new HttpRequestHeaders(this.nettyRequest);
        method = this.nettyRequest.getMethod();
        protocolVersion = this.nettyRequest.getProtocolVersion();
        this.contentSubject = contentSubject;
        uriInfoHolder = new UriInfoHolder(this.nettyRequest.getUri());
        cookiesHolder = CookiesHolder.newServerRequestHolder(nettyRequest.headers());
    }

    public HttpRequestHeaders getHeaders() {
        return headers;
    }

    public HttpMethod getHttpMethod() {
        return method;
    }

    public HttpVersion getHttpVersion() {
        return protocolVersion;
    }

    public String getUri() {
        return uriInfoHolder.getRawUriString();
    }

    public String getPath() {
        return uriInfoHolder.getPath();
    }

    public String getQueryString() {
        return uriInfoHolder.getQueryString();
    }

    public Map<String, List<String>> getQueryParameters() {
        return uriInfoHolder.getQueryParameters();
    }

    public Map<String, Set<Cookie>> getCookies() {
        return cookiesHolder.getAllCookies();
    }

    public Observable<T> getContent() {
        return contentSubject;
    }
    
}
