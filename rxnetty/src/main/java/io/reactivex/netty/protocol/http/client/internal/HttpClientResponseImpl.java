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
package io.reactivex.netty.protocol.http.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.protocol.http.CookiesHolder;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.sse.client.ServerSentEventDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

public final class HttpClientResponseImpl<T> extends HttpClientResponse<T> {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientResponseImpl.class);

    public static final String KEEP_ALIVE_HEADER_NAME = "Keep-Alive";
    private static final Pattern PATTERN_COMMA = Pattern.compile(",");
    private static final Pattern PATTERN_EQUALS = Pattern.compile("=");
    public static final String KEEP_ALIVE_TIMEOUT_HEADER_ATTR = "timeout";

    private final HttpResponse nettyResponse;
    private final Connection<?, ?> connection;
    private final CookiesHolder cookiesHolder;

    private HttpClientResponseImpl(HttpResponse nettyResponse) {
        this(nettyResponse, UnusableConnection.create());
    }

    private HttpClientResponseImpl(HttpResponse nettyResponse, Connection<?, ?> connection) {
        this.nettyResponse = nettyResponse;
        this.connection = connection;
        cookiesHolder = CookiesHolder.newClientResponseHolder(nettyResponse.headers());
    }

    @Override
    public HttpVersion getHttpVersion() {
        return nettyResponse.protocolVersion();
    }

    @Override
    public HttpResponseStatus getStatus() {
        return nettyResponse.status();
    }

    @Override
    public Map<String, Set<Cookie>> getCookies() {
        return cookiesHolder.getAllCookies();
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return nettyResponse.headers().contains(name);
    }

    @Override
    public boolean containsHeader(CharSequence name, CharSequence value, boolean ignoreCaseValue) {
        return nettyResponse.headers().contains(name, value, ignoreCaseValue);
    }

    @Override
    public Iterator<Entry<String, String>> headerIterator() {
        return nettyResponse.headers().iterator();
    }

    @Override
    public String getHeader(CharSequence name) {
        return nettyResponse.headers().get(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return nettyResponse.headers().get(name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return nettyResponse.headers().getAll(name);
    }

    @Override
    public long getContentLength() {
        return HttpHeaderUtil.getContentLength(nettyResponse);
    }

    @Override
    public long getContentLength(long defaultValue) {
        return HttpHeaderUtil.getContentLength(nettyResponse, defaultValue);
    }

    @Override
    public long getDateHeader(CharSequence name) {
        return nettyResponse.headers().getTimeMillis(name);
    }

    @Override
    public long getDateHeader(CharSequence name, long defaultValue) {
        return nettyResponse.headers().getTimeMillis(name, defaultValue);
    }

    @Override
    public String getHostHeader() {
        return nettyResponse.headers().get(HOST);
    }

    @Override
    public String getHost(String defaultValue) {
        return nettyResponse.headers().get(HOST, defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return nettyResponse.headers().getInt(name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return nettyResponse.headers().getInt(name, defaultValue);
    }

    @Override
    public boolean isContentLengthSet() {
        return HttpHeaderUtil.isContentLengthSet(nettyResponse);
    }

    @Override
    public boolean isKeepAlive() {
        return HttpHeaderUtil.isKeepAlive(nettyResponse);
    }

    @Override
    public boolean isTransferEncodingChunked() {
        return HttpHeaderUtil.isTransferEncodingChunked(nettyResponse);
    }

    @Override
    public Set<String> getHeaderNames() {
        return nettyResponse.headers().names();
    }

    @Override
    public HttpClientResponse<T> addHeader(CharSequence name, Object value) {
        nettyResponse.headers().add(name, value);
        return this;
    }

    @Override
    public HttpClientResponse<T> addCookie(Cookie cookie) {
        nettyResponse.headers().add(SET_COOKIE, ClientCookieEncoder.STRICT.encode(cookie));
        return this;
    }

    @Override
    public HttpClientResponse<T> addDateHeader(CharSequence name, Date value) {
        nettyResponse.headers().set(name, value);
        return this;
    }

    @Override
    public HttpClientResponse<T> addDateHeader(CharSequence name, Iterable<Date> values) {
        for (Date value : values) {
            nettyResponse.headers().add(name, value);
        }
        return this;
    }

    @Override
    public HttpClientResponse<T> addHeader(CharSequence name, Iterable<Object> values) {
        nettyResponse.headers().add(name, values);
        return this;
    }

    @Override
    public HttpClientResponse<T> setDateHeader(CharSequence name, Date value) {
        nettyResponse.headers().set(name, value);
        return this;
    }

    @Override
    public HttpClientResponse<T> setHeader(CharSequence name, Object value) {
        nettyResponse.headers().set(name, value);
        return this;
    }

    @Override
    public HttpClientResponse<T> setDateHeader(CharSequence name, Iterable<Date> values) {
        for (Date value : values) {
            nettyResponse.headers().set(name, value);
        }
        return this;
    }

    @Override
    public HttpClientResponse<T> setHeader(CharSequence name, Iterable<Object> values) {
        nettyResponse.headers().set(name, values);
        return this;
    }

    @Override
    public HttpClientResponse<T> removeHeader(CharSequence name) {
        nettyResponse.headers().remove(name);
        return this;
    }

    @Override
    public Observable<ServerSentEvent> getContentAsServerSentEvents() {
        if (containsHeader(CONTENT_TYPE, "text/event-stream", false)) {
            ChannelPipeline pipeline = unsafeNettyChannel().pipeline();
            ChannelHandlerContext decoderCtx = pipeline.context(HttpResponseDecoder.class);
            if (null != decoderCtx) {
                pipeline.addAfter(decoderCtx.name(), HandlerNames.SseClientCodec.getName(),
                                  new ServerSentEventDecoder());
            }
            return _contentObservable();
        }

        return Observable.error(new IllegalStateException("Response is not a server sent event response."));
    }

    @Override
    public Observable<T> getContent() {
        return _contentObservable();
    }

    protected <X> Observable<X> _contentObservable() {
        return Observable.create(new OnSubscribe<X>() {
            @Override
            public void call(Subscriber<? super X> subscriber) {
                unsafeNettyChannel().pipeline()
                                    .fireUserEventTriggered(new HttpContentSubscriberEvent<X>(subscriber));
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

    @Override
    public Channel unsafeNettyChannel() {
        return unsafeConnection().unsafeNettyChannel();
    }

    @Override
    public Connection<?, ?> unsafeConnection() {
        return connection;
    }

    /**
     * Parses the timeout value from the HTTP keep alive header (with name {@link #KEEP_ALIVE_HEADER_NAME}) as described in
     * <a href="http://tools.ietf.org/id/draft-thomson-hybi-http-timeout-01.html">this spec</a>
     *
     * @return The keep alive timeout or {@code null} if this response does not define the appropriate header value.
     */
    public Long getKeepAliveTimeoutSeconds() {
        String keepAliveHeader = nettyResponse.headers().get(KEEP_ALIVE_HEADER_NAME);
        if (null != keepAliveHeader && !keepAliveHeader.isEmpty()) {
            String[] pairs = PATTERN_COMMA.split(keepAliveHeader);
            if (pairs != null) {
                for (String pair: pairs) {
                    String[] nameValue = PATTERN_EQUALS.split(pair.trim());
                    if (nameValue != null && nameValue.length >= 2) {
                        String name = nameValue[0].trim().toLowerCase();
                        String value = nameValue[1].trim();
                        if (KEEP_ALIVE_TIMEOUT_HEADER_ATTR.equals(name)) {
                            try {
                                return Long.valueOf(value);
                            } catch (NumberFormatException e) {
                                logger.info("Invalid HTTP keep alive timeout value. Keep alive header: "
                                            + keepAliveHeader + ", timeout attribute value: " + nameValue[1], e);
                                return null;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /*Visible for the client bridge*/static <C> HttpClientResponseImpl<C> unsafeCreate(HttpResponse nettyResponse) {
        return new HttpClientResponseImpl<C>(nettyResponse);
    }

    public static <C> HttpClientResponse<C> newInstance(HttpClientResponse<C> unsafeInstance,
                                                        Connection<?, ?> connection) {
        HttpClientResponseImpl<C> cast = (HttpClientResponseImpl<C>) unsafeInstance;
        return new HttpClientResponseImpl<C>(cast.nettyResponse, connection);
    }

    public static <C> HttpClientResponse<C> newInstance(HttpResponse nettyResponse, Connection<?, ?> connection) {
        return new HttpClientResponseImpl<C>(nettyResponse, connection);
    }
}
