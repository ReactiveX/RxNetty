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
package io.reactivex.netty.protocol.http.clientNew;

import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.protocol.http.CookiesHolder;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.regex.Pattern;

public final class HttpClientResponseImpl<T> extends HttpClientResponse<T> {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientResponseImpl.class);

    public static final String KEEP_ALIVE_HEADER_NAME = "Keep-Alive";
    private static final Pattern PATTERN_COMMA = Pattern.compile(",");
    private static final Pattern PATTERN_EQUALS = Pattern.compile("=");
    public static final String KEEP_ALIVE_TIMEOUT_HEADER_ATTR = "timeout";

    private final HttpResponse nettyResponse;
    private final CookiesHolder cookiesHolder;
    private final Channel nettyChannel;

    protected HttpClientResponseImpl(HttpResponse nettyResponse, Channel nettyChannel) {
        this.nettyResponse = nettyResponse;
        this.nettyChannel = nettyChannel;
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
        return HttpHeaders.getHeader(nettyResponse, name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return nettyResponse.headers().getAll(name);
    }

    @Override
    public long getContentLength() {
        return HttpHeaders.getContentLength(nettyResponse);
    }

    @Override
    public long getContentLength(long defaultValue) {
        return HttpHeaders.getContentLength(nettyResponse, defaultValue);
    }

    @Override
    public Date getDateHeader(CharSequence name) throws ParseException {
        return HttpHeaders.getDateHeader(nettyResponse, name);
    }

    @Override
    public Date getDateHeader(CharSequence name, Date defaultValue) {
        return HttpHeaders.getDateHeader(nettyResponse, name, defaultValue);
    }

    @Override
    public String getHostHeader() {
        return HttpHeaders.getHost(nettyResponse);
    }

    @Override
    public String getHost(String defaultValue) {
        return HttpHeaders.getHost(nettyResponse, defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return HttpHeaders.getIntHeader(nettyResponse, name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return HttpHeaders.getIntHeader(nettyResponse, name, defaultValue);
    }

    @Override
    public boolean isContentLengthSet() {
        return HttpHeaders.isContentLengthSet(nettyResponse);
    }

    @Override
    public boolean isKeepAlive() {
        return HttpHeaders.isKeepAlive(nettyResponse);
    }

    @Override
    public boolean isTransferEncodingChunked() {
        return HttpHeaders.isTransferEncodingChunked(nettyResponse);
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
        nettyResponse.headers().add(HttpHeaders.Names.SET_COOKIE, ClientCookieEncoder.encode(cookie));
        return this;
    }

    @Override
    public HttpClientResponse<T> addDateHeader(CharSequence name, Date value) {
        HttpHeaders.addDateHeader(nettyResponse, name, value);
        return this;
    }

    @Override
    public HttpClientResponse<T> addDateHeader(CharSequence name, Iterable<Date> values) {
        for (Date value : values) {
            HttpHeaders.addDateHeader(nettyResponse, name, value);
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
        HttpHeaders.setDateHeader(nettyResponse, name, value);
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
            HttpHeaders.setDateHeader(nettyResponse, name, value);
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

    @Override
    DecoderResult decoderResult() {
        return nettyResponse.decoderResult();
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
}
