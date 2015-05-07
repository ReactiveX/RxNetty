/*
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.netty.protocol.http.serverNew;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.protocol.http.CookiesHolder;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import io.reactivex.netty.protocol.http.server.UriInfoHolder;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HttpServerRequestImpl<T> extends HttpServerRequest<T> {

    private final Channel nettyChannel;
    private final HttpRequest nettyRequest;
    private final CookiesHolder cookiesHolder;
    private final UriInfoHolder uriInfoHolder;

    public HttpServerRequestImpl(HttpRequest nettyRequest, Channel nettyChannel) {
        this.nettyRequest = nettyRequest;
        this.nettyChannel = nettyChannel;
        uriInfoHolder = new UriInfoHolder(this.nettyRequest.uri());
        cookiesHolder = CookiesHolder.newServerRequestHolder(nettyRequest.headers());
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
    public Iterator<Entry<String, String>> headerIterator() {
        return nettyRequest.headers().iterator();
    }

    @Override
    public String getHeader(CharSequence name) {
        return nettyRequest.headers().get(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return HttpHeaders.getHeader(nettyRequest, name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return nettyRequest.headers().getAll(name);
    }

    @Override
    public long getContentLength() {
        return HttpHeaders.getContentLength(nettyRequest);
    }

    @Override
    public long getContentLength(long defaultValue) {
        return HttpHeaders.getContentLength(nettyRequest, defaultValue);
    }

    @Override
    public Date getDateHeader(CharSequence name) throws ParseException {
        return HttpHeaders.getDateHeader(nettyRequest, name);
    }

    @Override
    public Date getDateHeader(CharSequence name, Date defaultValue) {
        return HttpHeaders.getDateHeader(nettyRequest, name, defaultValue);
    }

    @Override
    public String getHostHeader() {
        return HttpHeaders.getHost(nettyRequest);
    }

    @Override
    public String getHostHeader(String defaultValue) {
        return HttpHeaders.getHost(nettyRequest, defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return HttpHeaders.getIntHeader(nettyRequest, name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return HttpHeaders.getIntHeader(nettyRequest, name, defaultValue);
    }

    @Override
    public boolean is100ContinueExpected() {
        return HttpHeaders.is100ContinueExpected(nettyRequest);
    }

    @Override
    public boolean isContentLengthSet() {
        return HttpHeaders.isContentLengthSet(nettyRequest);
    }

    @Override
    public boolean isKeepAlive() {
        return HttpHeaders.isKeepAlive(nettyRequest);
    }

    @Override
    public boolean isTransferEncodingChunked() {
        return HttpHeaders.isTransferEncodingChunked(nettyRequest);
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
        nettyRequest.headers().add(Names.COOKIE,
                                   ClientCookieEncoder.encode(cookie) /*Since this is a request object, cookies are
                                   as if coming from a client*/);
        return this;

    }

    @Override
    public HttpServerRequest<T> addDateHeader(CharSequence name, Date value) {
        HttpHeaders.addDateHeader(nettyRequest, name, value);
        return this;
    }

    @Override
    public HttpServerRequest<T> addDateHeader(CharSequence name, Iterable<Date> values) {
        for (Date value : values) {
            HttpHeaders.addDateHeader(nettyRequest, name, value);
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
        HttpHeaders.setDateHeader(nettyRequest, name, value);
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
            HttpHeaders.setDateHeader(nettyRequest, name, value);
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
    public Observable<T> getContent() {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                nettyChannel.pipeline()
                            .fireUserEventTriggered(new HttpContentSubscriberEvent<T>(subscriber));
            }
        });
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
}
