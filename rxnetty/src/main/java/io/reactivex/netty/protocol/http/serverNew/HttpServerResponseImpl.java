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

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class HttpServerResponseImpl<C> extends HttpServerResponse<C> {

    private final HttpResponse headers;

    @SuppressWarnings("rawtypes")
    private final Connection connection;
    private boolean headersSent; /*Class is not thread safe*/

    HttpServerResponseImpl(@SuppressWarnings("rawtypes") Connection connection, HttpResponse headers) {
        this.connection = connection;
        this.headers = headers;
    }

    @Override
    public HttpResponseStatus getStatus() {
        return headers.status();
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return headers.headers().contains(name);
    }

    @Override
    public boolean containsHeader(CharSequence name, CharSequence value, boolean ignoreCaseValue) {
        return headers.headers().contains(name, value, ignoreCaseValue);
    }

    @Override
    public String getHeader(CharSequence name) {
        return headers.headers().get(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return HttpHeaders.getHeader(headers, name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return headers.headers().getAll(name);
    }

    @Override
    public Date getDateHeader(CharSequence name) throws ParseException {
        return HttpHeaders.getDateHeader(headers, name);
    }

    @Override
    public Date getDateHeader(CharSequence name, Date defaultValue) {
        return HttpHeaders.getDateHeader(headers, name, defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return HttpHeaders.getIntHeader(headers, name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return HttpHeaders.getIntHeader(headers, name, defaultValue);
    }

    @Override
    public Set<String> getHeaderNames() {
        return headers.headers().names();
    }

    @Override
    public HttpServerResponse<C> addHeader(CharSequence name, Object value) {
        if (allowUpdate()) {
            headers.headers().add(name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addCookie(Cookie cookie) {
        if (allowUpdate()) {
            headers.headers().add(Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addDateHeader(CharSequence name, Date value) {
        if (allowUpdate()) {
            HttpHeaders.addDateHeader(headers, name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addDateHeader(CharSequence name, Iterable<Date> values) {
        if (allowUpdate()) {
            for (Date value : values) {
                HttpHeaders.addDateHeader(headers, name, value);
            }
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addHeader(CharSequence name, Iterable<Object> values) {
        if (allowUpdate()) {
            headers.headers().add(name, values);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setDateHeader(CharSequence name, Date value) {
        if (allowUpdate()) {
            HttpHeaders.setDateHeader(headers, name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setHeader(CharSequence name, Object value) {
        if (allowUpdate()) {
            HttpHeaders.setHeader(headers, name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setDateHeader(CharSequence name, Iterable<Date> values) {
        if (allowUpdate()) {
            for (Date value : values) {
                HttpHeaders.setDateHeader(headers, name, value);
            }
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setHeader(CharSequence name, Iterable<Object> values) {
        if (allowUpdate()) {
            headers.headers().set(name, values);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> removeHeader(CharSequence name) {
        if (allowUpdate()) {
            headers.headers().remove(name);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setStatus(HttpResponseStatus status) {
        if (allowUpdate()) {
            headers.setStatus(status);
        }
        return this;
    }

    @Override
    public ContentWriter<C> sendHeaders() {
        if (allowUpdate()) {
            headersSent = true;
            return new ContentWriterImpl<>(connection, headers);
        }

        return new FailedContentWriter<>();
    }

    private boolean allowUpdate() {
        return !headersSent;
    }

    private static class FailedContentWriter<C> extends ContentWriter<C> {

        private FailedContentWriter() {
            super(new OnSubscribe<Void>() {
                @Override
                public void call(Subscriber<? super Void> subscriber) {
                    subscriber.onError(new IllegalStateException("HTTP headers are already sent."));
                }
            });
        }

        @Override
        public ContentWriter<C> write(Observable<C> msgs) {
            return this;
        }

        @Override
        public <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource,
                                                                  Func0<T> trailerFactory,
                                                                  Func2<T, C, T> trailerMutator) {
            return this;
        }

        @Override
        public <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource, Func0<T> trailerFactory,
                                                                  Func2<T, C, T> trailerMutator,
                                                                  Func1<C, Boolean> flushSelector) {
            return this;
        }

        @Override
        public ContentWriter<C> write(Observable<C> msgs, Func1<C, Boolean> flushSelector) {
            return this;
        }

        @Override
        public ContentWriter<C> writeAndFlushOnEach(Observable<C> msgs) {
            return this;
        }

        @Override
        public ContentWriter<C> writeString(Observable<String> msgs) {
            return this;
        }

        @Override
        public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                        Func0<T> trailerFactory,
                                                                        Func2<T, String, T> trailerMutator) {
            return this;
        }

        @Override
        public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                        Func0<T> trailerFactory,
                                                                        Func2<T, String, T> trailerMutator,
                                                                        Func1<String, Boolean> flushSelector) {
            return this;
        }

        @Override
        public ContentWriter<C> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector) {
            return this;
        }

        @Override
        public ContentWriter<C> writeStringAndFlushOnEach(Observable<String> msgs) {
            return this;
        }

        @Override
        public ContentWriter<C> writeBytes(Observable<byte[]> msgs) {
            return this;
        }

        @Override
        public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                       Func0<T> trailerFactory,
                                                                       Func2<T, byte[], T> trailerMutator) {
            return this;
        }

        @Override
        public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                       Func0<T> trailerFactory,
                                                                       Func2<T, byte[], T> trailerMutator,
                                                                       Func1<byte[], Boolean> flushSelector) {
            return this;
        }

        @Override
        public ContentWriter<C> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector) {
            return this;
        }

        @Override
        public ContentWriter<C> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
            return this;
        }
    }
}
