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
package io.reactivex.netty.protocol.http.ws.client.internal;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ContentSource;
import io.reactivex.netty.client.ClientConnectionToChannelBridge;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import rx.Observable;
import rx.Observable.Transformer;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class WebSocketResponseImpl<T> extends WebSocketResponse<T> {

    private final HttpClientResponse<T> delegate;
    private final WebSocketConnection wsConnection;
    private final Channel channel;

    public WebSocketResponseImpl(HttpClientResponse<T> delegate) {
        this.delegate = delegate;
        @SuppressWarnings("unchecked")
        Connection<WebSocketFrame, WebSocketFrame> cast =
                (Connection<WebSocketFrame, WebSocketFrame>) delegate.unsafeConnection();
        channel = cast.unsafeNettyChannel();
        wsConnection = new WebSocketConnection(cast);
    }

    @Override
    public Observable<WebSocketConnection> getWebSocketConnection() {
        if (isUpgraded()) {
            /*Do not pool connection once upgraded to WS. A closing handshake closes the channel*/
            channel.attr(ClientConnectionToChannelBridge.DISCARD_CONNECTION).set(true);
            channel.attr(AbstractHttpConnectionBridge.CONNECTION_UPGRADED).set(true);
            return Observable.just(wsConnection);
        } else {
            return Observable.error(new IllegalStateException("WebSocket upgrade rejected by the server."));
        }
    }

    @Override
    public HttpVersion getHttpVersion() {
        return delegate.getHttpVersion();
    }

    @Override
    public HttpResponseStatus getStatus() {
        return delegate.getStatus();
    }

    @Override
    public Map<String, Set<Cookie>> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return delegate.containsHeader(name);
    }

    @Override
    public boolean containsHeader(CharSequence name, CharSequence value, boolean ignoreCaseValue) {
        return delegate.containsHeader(name, value, ignoreCaseValue);
    }

    @Override
    public Iterator<Entry<CharSequence, CharSequence>> headerIterator() {
        return delegate.headerIterator();
    }

    @Override
    public String getHeader(CharSequence name) {
        return delegate.getHeader(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return delegate.getHeader(name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return delegate.getAllHeaderValues(name);
    }

    @Override
    public long getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public long getContentLength(long defaultValue) {
        return delegate.getContentLength(defaultValue);
    }

    @Override
    public long getDateHeader(CharSequence name) throws ParseException {
        return delegate.getDateHeader(name);
    }

    @Override
    public long getDateHeader(CharSequence name, long defaultValue) {
        return delegate.getDateHeader(name, defaultValue);
    }

    @Override
    public String getHostHeader() {
        return delegate.getHostHeader();
    }

    @Override
    public String getHost(String defaultValue) {
        return delegate.getHost(defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return delegate.getIntHeader(name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return delegate.getIntHeader(name, defaultValue);
    }

    @Override
    public boolean isContentLengthSet() {
        return delegate.isContentLengthSet();
    }

    @Override
    public boolean isKeepAlive() {
        return delegate.isKeepAlive();
    }

    @Override
    public boolean isTransferEncodingChunked() {
        return delegate.isTransferEncodingChunked();
    }

    @Override
    public Set<String> getHeaderNames() {
        return delegate.getHeaderNames();
    }

    @Override
    public HttpClientResponse<T> addHeader(CharSequence name,
                                           Object value) {
        return delegate.addHeader(name, value);
    }

    @Override
    public HttpClientResponse<T> addCookie(
            Cookie cookie) {
        return delegate.addCookie(cookie);
    }

    @Override
    public HttpClientResponse<T> addDateHeader(CharSequence name,
                                               Date value) {
        return delegate.addDateHeader(name, value);
    }

    @Override
    public HttpClientResponse<T> addDateHeader(CharSequence name,
                                               Iterable<Date> values) {
        return delegate.addDateHeader(name, values);
    }

    @Override
    public HttpClientResponse<T> addHeader(CharSequence name,
                                           Iterable<Object> values) {
        return delegate.addHeader(name, values);
    }

    @Override
    public HttpClientResponse<T> setDateHeader(CharSequence name,
                                               Date value) {
        return delegate.setDateHeader(name, value);
    }

    @Override
    public HttpClientResponse<T> setHeader(CharSequence name,
                                           Object value) {
        return delegate.setHeader(name, value);
    }

    @Override
    public HttpClientResponse<T> setDateHeader(CharSequence name,
                                               Iterable<Date> values) {
        return delegate.setDateHeader(name, values);
    }

    @Override
    public HttpClientResponse<T> setHeader(CharSequence name,
                                           Iterable<Object> values) {
        return delegate.setHeader(name, values);
    }

    @Override
    public HttpClientResponse<T> removeHeader(CharSequence name) {
        return delegate.removeHeader(name);
    }

    @Override
    public ContentSource<ServerSentEvent> getContentAsServerSentEvents() {
        return delegate.getContentAsServerSentEvents();
    }

    @Override
    public ContentSource<T> getContent() {
        return delegate.getContent();
    }

    @Override
    public Observable<Void> discardContent() {
        return delegate.discardContent();
    }

    @Override
    public <TT> HttpClientResponse<TT> transformContent(Transformer<T, TT> transformer) {
        return delegate.transformContent(transformer);
    }

    @Override
    public Channel unsafeNettyChannel() {
        return delegate.unsafeNettyChannel();
    }

    @Override
    public Connection<?, ?> unsafeConnection() {
        return delegate.unsafeConnection();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
