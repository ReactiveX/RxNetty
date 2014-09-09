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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.protocol.http.AbstractHttpContentHolder;
import io.reactivex.netty.protocol.http.CookiesHolder;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <h2>Handling Content</h2>
 * This request supports delayed subscription to content (if expected). Although this is useful, it incurs an overhead
 * of buffering the content till the subscription arrives. In order to reduce this overhead and safety against memory
 * leaks the following restrictions are imposed on the users:
 *
 * <ul>
 <li>The content {@link Observable} as returned by {@link #getContent()} supports one and only one subscriber.</li>
 <li>It is mandatory to either call {@link #ignoreContent()} or have atleast one subscription to {@link #getContent()}</li>
 <li>Any subscriptions to {@link #getContent()} after the configured timeout, will result in error on the subscriber.</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public class HttpServerRequest<T> extends AbstractHttpContentHolder<T> {

    public static final long DEFAULT_CONTENT_SUBSCRIPTION_TIMEOUT_MS = 1;

    private final HttpRequest nettyRequest;
    private final HttpRequestHeaders headers;
    private final HttpMethod method;
    private final HttpVersion protocolVersion;
    private final UriInfoHolder uriInfoHolder;
    private final CookiesHolder cookiesHolder;
    private final Channel nettyChannel;
    private volatile long processingStartTimeMillis;

    /**
     * @deprecated This class will no longer be instantiable outside this package.
     */
    @Deprecated
    public HttpServerRequest(HttpRequest nettyRequest, UnicastContentSubject<T> content) {
        super(content);
        this.nettyRequest = nettyRequest;
        headers = new HttpRequestHeaders(this.nettyRequest);
        method = this.nettyRequest.getMethod();
        protocolVersion = this.nettyRequest.getProtocolVersion();
        uriInfoHolder = new UriInfoHolder(this.nettyRequest.getUri());
        cookiesHolder = CookiesHolder.newServerRequestHolder(nettyRequest.headers());
        nettyChannel = null;
    }

    protected HttpServerRequest(Channel channel, HttpRequest nettyRequest,
                                UnicastContentSubject<T> content) {
        super(content);
        nettyChannel = channel;
        this.nettyRequest = nettyRequest;
        headers = new HttpRequestHeaders(this.nettyRequest);
        method = this.nettyRequest.getMethod();
        protocolVersion = this.nettyRequest.getProtocolVersion();
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

    public Channel getNettyChannel() {
        return nettyChannel;
    }

    void onProcessingStart(long processingStartTimeMillis) {
        this.processingStartTimeMillis = processingStartTimeMillis;
    }

    long onProcessingEnd() {
        return Clock.onEndMillis(processingStartTimeMillis);
    }
    
}
