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
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.serialization.ByteTransformer;

import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public class HttpRequest<T> {

    private final io.netty.handler.codec.http.HttpRequest nettyRequest;
    private final HttpRequestHeaders headers;
    private final RawContentSource<?> rawContentSource;
    private final ContentSource<T> contentSource;

    private HttpRequest(io.netty.handler.codec.http.HttpRequest nettyRequest, HttpRequestHeaders headers,
                        ContentSource<T> contentSource) {
        this.nettyRequest = nettyRequest;
        this.headers = headers;
        this.contentSource = contentSource;
        rawContentSource = null;
    }

    private HttpRequest(io.netty.handler.codec.http.HttpRequest nettyRequest, HttpRequestHeaders headers,
                        RawContentSource<?> rawContentSource) {
        this.nettyRequest = nettyRequest;
        this.headers = headers;
        this.rawContentSource = rawContentSource;
        contentSource = null;
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

    io.netty.handler.codec.http.HttpRequest getNettyRequest() {
        return nettyRequest;
    }

    ContentSource<T> getContentSource() {
        return contentSource;
    }

    RawContentSource<?> getRawContentSource() {
        return rawContentSource;
    }

    boolean hasRawContentSource() {
        return null != rawContentSource;
    }

    public static final class RequestBuilder<T> {

        private final io.netty.handler.codec.http.HttpRequest nettyRequest;
        private final HttpRequestHeaders headers;
        private ContentSource<T> contentSource = new ContentSource.EmptySource<T>();
        private RawContentSource<?> rawContentSource;

        public RequestBuilder(HttpVersion httpVersion, HttpMethod method, String uri) {
            nettyRequest = new DefaultHttpRequest(httpVersion, method, uri);
            headers = new HttpRequestHeaders(nettyRequest);
        }

        public RequestBuilder(HttpMethod method, String uri) {
            this(HttpVersion.HTTP_1_1, method, uri);
        }

        public RequestBuilder<T> withHeader(String name, String value) {
            headers.add(name, value);
            return this;
        }

        public RequestBuilder<T> withCookie(@SuppressWarnings("unused") Cookie cookie) {
            //TODO: Cookie handling.
            return this;
        }

        public RequestBuilder<T> withContentSource(ContentSource<T> contentSource) {
            this.contentSource = contentSource;
            return this;
        }

        public <R> RequestBuilder<T> withRawContentSource(RawContentSource<R> rawContentSource) {
            this.rawContentSource = rawContentSource;
            return this;
        }

        public RequestBuilder<T> withContent(T content) {
            if (!headers.isContentLengthSet()) {
                headers.set(HttpHeaders.Names.TRANSFER_ENCODING, "chunked");
            }
            contentSource = new ContentSource.SingletonSource<T>(content);
            return this;
        }

        public RequestBuilder<T> withContent(String content) {
            return withContent(content.getBytes(Charset.defaultCharset()));
        }

        public RequestBuilder<T> withContent(byte[] content) {
            headers.set(HttpHeaders.Names.CONTENT_LENGTH, content.length);
            rawContentSource = new RawContentSource.SingletonRawSource<byte[]>(content, new ByteTransformer());
            return this;
        }

        public HttpRequest<T> build() {
            if (null != rawContentSource) {
                return new HttpRequest<T>(nettyRequest, headers, rawContentSource);
            }
            return new HttpRequest<T>(nettyRequest, headers, contentSource);
        }
    }
}
