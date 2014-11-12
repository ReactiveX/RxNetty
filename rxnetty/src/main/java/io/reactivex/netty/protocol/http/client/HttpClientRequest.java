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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.channel.ByteTransformer;
import io.reactivex.netty.channel.ContentTransformer;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public class HttpClientRequest<T> {

    private final HttpRequest nettyRequest;
    private final HttpRequestHeaders headers;
    private Observable<T> contentSource;
    private Observable<ByteBuf> rawContentSource;
    private String absoluteUri;
    private Action0 onWriteCompleteAction;

    HttpClientRequest(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        headers = new HttpRequestHeaders(nettyRequest);
    }

    /**
     * Does a shallow copy of the passed request instance.
     *
     * @param shallowCopySource Request to make a shallow copy from.
     */
    HttpClientRequest(HttpClientRequest<T> shallowCopySource) {
        nettyRequest = shallowCopySource.nettyRequest;
        headers = new HttpRequestHeaders(nettyRequest);
        contentSource = shallowCopySource.contentSource;
        rawContentSource = shallowCopySource.rawContentSource;
        absoluteUri = shallowCopySource.absoluteUri;
    }

    /**
     * Does a shallow copy of the passed request instance with a new netty request.
     *
     * @param nettyRequest New netty request for this request.
     * @param shallowCopySource Request to make a shallow copy from.
     */
    HttpClientRequest(HttpRequest nettyRequest, HttpClientRequest<T> shallowCopySource) {
        this.nettyRequest = nettyRequest;
        headers = new HttpRequestHeaders(nettyRequest);
        contentSource = shallowCopySource.contentSource;
        rawContentSource = shallowCopySource.rawContentSource;
        absoluteUri = shallowCopySource.absoluteUri;
    }

    public static HttpClientRequest<ByteBuf> createGet(String uri) {
        return create(HttpMethod.GET, uri);
    }

    public static HttpClientRequest<ByteBuf> createPost(String uri) {
        return create(HttpMethod.POST, uri);
    }

    public static HttpClientRequest<ByteBuf> createPut(String uri) {
        return create(HttpMethod.PUT, uri);
    }

    public static HttpClientRequest<ByteBuf> createDelete(String uri) {
        return create(HttpMethod.DELETE, uri);
    }

    public static <T> HttpClientRequest<T> create(HttpVersion httpVersion, HttpMethod method, String uri) {
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(httpVersion, method, uri);
        return new HttpClientRequest<T>(nettyRequest);
    }

    public static <T> HttpClientRequest<T> create(HttpMethod method, String uri) {
        return create(HttpVersion.HTTP_1_1, method, uri);
    }

    public HttpClientRequest<T> withHeader(String name, String value) {
        headers.add(name, value);
        return this;
    }

    public HttpClientRequest<T> withCookie(Cookie cookie) {
        String cookieHeader = ClientCookieEncoder.encode(cookie);
        return withHeader(HttpHeaders.Names.COOKIE, cookieHeader);
    }

    public HttpClientRequest<T> withContentSource(Observable<T> contentSource) {
        this.contentSource = contentSource;
        return this;
    }

    public <S> HttpClientRequest<T> withRawContentSource(final Observable<S> rawContentSource,
                                                         final ContentTransformer<S> transformer) {
        this.rawContentSource = rawContentSource.map(new Func1<S, ByteBuf>() {
            @Override
            public ByteBuf call(S rawContent) {
                return transformer.call(rawContent, PooledByteBufAllocator.DEFAULT);
            }
        });
        return this;
    }

    public <S> HttpClientRequest<T> withRawContent(S content, final ContentTransformer<S> transformer) {
        return withRawContentSource(Observable.just(content), transformer);
    }

    public HttpClientRequest<T> withContent(T content) {
        withContentSource(Observable.just(content));
        return this;
    }

    public HttpClientRequest<T> withContent(String content) {
        return withContent(content.getBytes(Charset.defaultCharset()));
    }

    public HttpClientRequest<T> withContent(byte[] content) {
        headers.set(HttpHeaders.Names.CONTENT_LENGTH, content.length);
        withRawContentSource(Observable.just(content), ByteTransformer.DEFAULT_INSTANCE);
        return this;
    }

    public HttpRequestHeaders getHeaders() {
        return headers;
    }

    public HttpVersion getHttpVersion() {
        return nettyRequest.getProtocolVersion();
    }

    public HttpMethod getMethod() {
        return nettyRequest.getMethod();
    }

    public String getUri() {
        return nettyRequest.getUri();
    }

    public String getAbsoluteUri() {
        return null != absoluteUri ? absoluteUri : getUri();
    }

    HttpRequest getNettyRequest() {
        return nettyRequest;
    }

    enum ContentSourceType { Raw, Typed, Absent }

    ContentSourceType getContentSourceType() {
        return null == contentSource
               ? null == rawContentSource ? ContentSourceType.Absent : ContentSourceType.Raw
               : ContentSourceType.Typed;
    }

    Observable<T> getContentSource() {
        return contentSource;
    }

    Observable<ByteBuf> getRawContentSource() {
        return rawContentSource;
    }

    void removeContent() {
        contentSource = null;
        rawContentSource = null;
    }

    void doOnWriteComplete(Action0 onWriteCompleteAction) {
        this.onWriteCompleteAction = onWriteCompleteAction;
    }

    void onWriteComplete() {
        if (null != onWriteCompleteAction) {
            onWriteCompleteAction.call();
        }
    }

    /*Set by HttpClient*/void setDynamicUriParts(String host, int port, boolean secure) {
        absoluteUri = secure ? "https" : "http" + "://" + host + ':' + port; // Uri in netty always starts with a slash
    }
}
