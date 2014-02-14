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
import io.netty.handler.codec.http.HttpVersion;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.util.functions.Func1;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nitesh Kant
 */
public class HttpRequest<T> {

    private final io.netty.handler.codec.http.HttpRequest nettyRequest;
    private final HttpRequestHeaders headers;
    private final PublishSubject<T> contentSubject;
    private final HttpMethod method;
    private final HttpVersion protocolVersion;
    private final String uri;

    public HttpRequest(io.netty.handler.codec.http.HttpRequest nettyRequest, PublishSubject<T> contentSubject) {
        this.nettyRequest = nettyRequest;
        headers = new HttpRequestHeaders(this.nettyRequest);
        method = this.nettyRequest.getMethod();
        protocolVersion = this.nettyRequest.getProtocolVersion();
        uri = this.nettyRequest.getUri();
        this.contentSubject = contentSubject;
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
        return uri;
    }

    public String getPath() {
        // TODO: Parse URI
        return null;
    }

    public String getQueryString() {
        // TODO: Parse URI
        return null;
    }

    public Map<String, List<String>> getQueryParameters() {
        // TODO: Parse URI
        return null;
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
