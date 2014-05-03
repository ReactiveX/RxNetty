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
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.serialization.ByteTransformer;

import java.nio.charset.Charset;

import static io.reactivex.netty.protocol.http.client.RawContentSource.SingletonRawSource;

/**
 * @author Nitesh Kant
 */
public class HttpClientRequest<T> {

    static class SimpleContentSourceFactory<T, R extends ContentSource<T>> implements ContentSourceFactory<T, R> {

        private final R source;
        
        SimpleContentSourceFactory(R source) {
            this.source = source;    
        }
        @Override
        public R newContentSource() {
            return source;
        }
    }

    private final HttpRequest nettyRequest;
    private final HttpRequestHeaders headers;
    protected ContentSourceFactory<T, ContentSource<T>> contentFactory;
    protected ContentSourceFactory<?, RawContentSource<?>> rawContentFactory;
    protected boolean userPassedInFactory;

    /*Visible for testing*/ HttpClientRequest(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        headers = new HttpRequestHeaders(nettyRequest);
        contentFactory = null;
        rawContentFactory = null;
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

    public HttpClientRequest<T> withContentSource(ContentSource<T> contentSource) {
        setContentFactory(new SimpleContentSourceFactory<T, ContentSource<T>>(contentSource));
        return this;
    }
    
    public HttpClientRequest<T> withContentFactory(ContentSourceFactory<T, ContentSource<T>> contentFactory) {
        userPassedInFactory = true;
        setContentFactory(contentFactory);
        return this;
    }
    
    public HttpClientRequest<T> withRawContentFactory(ContentSourceFactory<?, RawContentSource<?>> rawContentFactory) {
        userPassedInFactory = true;
        setRawContentFactory(rawContentFactory);
        return this;
    }

    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public HttpClientRequest<T> withRawContentSource(final RawContentSource<?> rawContentSource) {
        setRawContentFactory(new SimpleContentSourceFactory(rawContentSource));
        return this;
    }

    public HttpClientRequest<T> withContent(T content) {
        setContentFactory(new SimpleContentSourceFactory<T, ContentSource<T>>(new ContentSource.SingletonSource<T>(content)));
        return this;
    }

    public HttpClientRequest<T> withContent(String content) {
        return withContent(content.getBytes(Charset.defaultCharset()));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HttpClientRequest<T> withContent(byte[] content) {
        headers.set(HttpHeaders.Names.CONTENT_LENGTH, content.length);
        setRawContentFactory(new SimpleContentSourceFactory(new SingletonRawSource<byte[]>(content, new ByteTransformer())));
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

    private void setRawContentFactory(ContentSourceFactory<?, RawContentSource<?>> rawContentFactory) {
        // raw content factory and content factory is mutually exclusive
        this.rawContentFactory = rawContentFactory;
        this.contentFactory = null;
    }
    
    private void setContentFactory(ContentSourceFactory<T, ContentSource<T>> contentFactory) {
        // raw content factory and content factory is mutually exclusive
        this.contentFactory = contentFactory;
        this.rawContentFactory = null;
    }

    HttpRequest getNettyRequest() {
        return nettyRequest;
    }

    boolean hasRawContentSource() {
        return rawContentFactory != null;
    }
    
    boolean hasContentSource() {
        return rawContentFactory != null || contentFactory != null;
    }
    
    ContentSource<T> getContentSource() {
        return contentFactory.newContentSource();
    }
    
    RawContentSource<?> getRawContentSource() {
        return rawContentFactory.newContentSource();
    }
}
