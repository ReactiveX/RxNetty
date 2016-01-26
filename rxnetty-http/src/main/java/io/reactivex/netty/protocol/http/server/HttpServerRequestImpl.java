/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */
package io.reactivex.netty.protocol.http.server;

import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.channel.ContentSource;
import io.reactivex.netty.protocol.http.CookiesHolder;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import io.reactivex.netty.protocol.http.ws.server.WebSocketHandshaker;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

class HttpServerRequestImpl<T> extends HttpServerRequest<T> {

    private final Channel nettyChannel;
    private final HttpRequest nettyRequest;
    private final CookiesHolder cookiesHolder;
    private final UriInfoHolder uriInfoHolder;
    private final ContentSource<T> contentSource;

    HttpServerRequestImpl(HttpRequest nettyRequest, Channel nettyChannel) {
        this.nettyRequest = nettyRequest;
        this.nettyChannel = nettyChannel;
        uriInfoHolder = new UriInfoHolder(this.nettyRequest.uri());
        cookiesHolder = CookiesHolder.newServerRequestHolder(nettyRequest.headers());
        contentSource = new ContentSource<>(nettyChannel, new Func1<Subscriber<? super T>, Object>() {
            @Override
            public Object call(Subscriber<? super T> subscriber) {
                return new HttpContentSubscriberEvent<>(subscriber);
            }
        });
    }

    private HttpServerRequestImpl(HttpRequest nettyRequest, Channel nettyChannel, ContentSource<T> contentSource) {
        this.nettyRequest = nettyRequest;
        this.nettyChannel = nettyChannel;
        uriInfoHolder = new UriInfoHolder(this.nettyRequest.uri());
        cookiesHolder = CookiesHolder.newServerRequestHolder(nettyRequest.headers());
        this.contentSource = contentSource;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return nettyRequest.method();
    }

    @Override
    public HttpVersion getHttpVersion() {
        return nettyRequest.protocolVersion();
    }

    @Override
    public String getUri() {
        return uriInfoHolder.getRawUriString();
    }

    @Override
    public String getDecodedPath() {
        return uriInfoHolder.getPath();
    }

    @Override
    public String getRawQueryString() {
        return uriInfoHolder.getQueryString();
    }

    @Override
    public Map<String, Set<Cookie>> getCookies() {
        return cookiesHolder.getAllCookies();
    }

    @Override
    public Map<String, List<String>> getQueryParameters() {
        return uriInfoHolder.getQueryParameters();
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return nettyRequest.headers().contains(name);
    }

    @Override
    public boolean containsHeader(CharSequence name, CharSequence value, boolean ignoreCaseValue) {
        return nettyRequest.headers().contains(name, value, ignoreCaseValue);
    }

    @Override
    public Iterator<Entry<CharSequence, CharSequence>> headerIterator() {
        return nettyRequest.headers().iteratorCharSequence();
    }

    @Override
    public String getHeader(CharSequence name) {
        return nettyRequest.headers().get(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return nettyRequest.headers().get(name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return nettyRequest.headers().getAll(name);
    }

    @Override
    public long getContentLength() {
        return HttpUtil.getContentLength(nettyRequest);
    }

    @Override
    public long getContentLength(long defaultValue) {
        return HttpUtil.getContentLength(nettyRequest, defaultValue);
    }

    @Override
    public long getDateHeader(CharSequence name) {
        return nettyRequest.headers().getTimeMillis(name);
    }

    @Override
    public long getDateHeader(CharSequence name, long defaultValue) {
        return nettyRequest.headers().getTimeMillis(name, defaultValue);
    }

    @Override
    public String getHostHeader() {
        return nettyRequest.headers().get(HOST);
    }

    @Override
    public String getHostHeader(String defaultValue) {
        return nettyRequest.headers().get(HOST, defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return nettyRequest.headers().getInt(name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return nettyRequest.headers().getInt(name, defaultValue);
    }

    @Override
    public boolean is100ContinueExpected() {
        return HttpUtil.is100ContinueExpected(nettyRequest);
    }

    @Override
    public boolean isContentLengthSet() {
        return HttpUtil.isContentLengthSet(nettyRequest);
    }

    @Override
    public boolean isKeepAlive() {
        return HttpUtil.isKeepAlive(nettyRequest);
    }

    @Override
    public boolean isTransferEncodingChunked() {
        return HttpUtil.isTransferEncodingChunked(nettyRequest);
    }

    @Override
    public Set<String> getHeaderNames() {
        return nettyRequest.headers().names();
    }

    @Override
    public HttpServerRequest<T> addHeader(CharSequence name, Object value) {
        nettyRequest.headers().add(name, value);
        return this;
    }

    @Override
    public HttpServerRequest<T> addCookie(Cookie cookie) {
        nettyRequest.headers().add(COOKIE,
                                   ClientCookieEncoder.STRICT.encode(cookie) /*Since this is a request object, cookies are
                                   as if coming from a client*/);
        return this;

    }

    @Override
    public HttpServerRequest<T> addDateHeader(CharSequence name, Date value) {
        nettyRequest.headers().add(name, value);
        return this;
    }

    @Override
    public HttpServerRequest<T> addDateHeader(CharSequence name, Iterable<Date> values) {
        for (Date value : values) {
            nettyRequest.headers().add(name, value);
        }
        return this;
    }

    @Override
    public HttpServerRequest<T> addHeader(CharSequence name, Iterable<Object> values) {
        nettyRequest.headers().add(name, values);
        return this;
    }

    @Override
    public HttpServerRequest<T> setDateHeader(CharSequence name, Date value) {
        nettyRequest.headers().set(name, value);
        return this;
    }

    @Override
    public HttpServerRequest<T> setHeader(CharSequence name, Object value) {
        nettyRequest.headers().set(name, value);
        return this;
    }

    @Override
    public HttpServerRequest<T> setDateHeader(CharSequence name, Iterable<Date> values) {
        for (Date value : values) {
            nettyRequest.headers().set(name, value);
        }
        return this;
    }

    @Override
    public HttpServerRequest<T> setHeader(CharSequence name, Iterable<Object> values) {
        nettyRequest.headers().add(name, values);
        return this;
    }

    @Override
    public HttpServerRequest<T> removeHeader(CharSequence name) {
        nettyRequest.headers().remove(name);
        return this;
    }

    @Override
    public ContentSource<T> getContent() {
        return contentSource;
    }

    @Override
    public Observable<Void> discardContent() {
        return getContent().map(new Func1<T, Void>() {
            @Override
            public Void call(T t) {
                ReferenceCountUtil.release(t);
                return null;
            }
        }).ignoreElements();
    }

    @Override
    public Observable<Void> dispose() {
        return discardContent().onErrorResumeNext(Observable.<Void>empty());
    }

    @Override
    public boolean isWebSocketUpgradeRequested() {
        return WebSocketHandshaker.isUpgradeRequested(this);
    }

    @Override
    public <X> HttpServerRequest<X> transformContent(Transformer<T, X> transformer) {
        return new HttpServerRequestImpl<>(nettyRequest, nettyChannel, contentSource.transform(transformer));
    }

    @Override
    DecoderResult decoderResult() {
        return nettyRequest.decoderResult();
    }
}
