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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.reactivex.netty.channel.AllocatingTransformer;
import io.reactivex.netty.channel.ChannelOperations;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.MarkAwarePipeline;
import io.reactivex.netty.protocol.http.HttpHandlerNames;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.sse.server.ServerSentEventEncoder;
import io.reactivex.netty.protocol.http.ws.server.WebSocketHandler;
import io.reactivex.netty.protocol.http.ws.server.WebSocketHandshaker;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.Date;
import java.util.List;
import java.util.Set;

public final class HttpServerResponseImpl<C> extends HttpServerResponse<C> {

    private final State<C> state;

    private HttpServerResponseImpl(final State<C> state) {
        super(new OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                state.sendHeaders().write(Observable.<C>empty()).unsafeSubscribe(subscriber);
            }
        });
        this.state = state;
    }

    @Override
    public HttpResponseStatus getStatus() {
        return state.headers.status();
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return state.headers.headers().contains(name);
    }

    @Override
    public boolean containsHeader(CharSequence name, CharSequence value, boolean ignoreCaseValue) {
        return state.headers.headers().contains(name, value, ignoreCaseValue);
    }

    @Override
    public String getHeader(CharSequence name) {
        return state.headers.headers().get(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return state.headers.headers().get(name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return state.headers.headers().getAll(name);
    }

    @Override
    public long getDateHeader(CharSequence name) {
        return state.headers.headers().getTimeMillis(name);
    }

    @Override
    public long getDateHeader(CharSequence name, long defaultValue) {
        return state.headers.headers().getTimeMillis(name, defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return state.headers.headers().getInt(name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return state.headers.headers().getInt(name, defaultValue);
    }

    @Override
    public Set<String> getHeaderNames() {
        return state.headers.headers().names();
    }

    @Override
    public HttpServerResponse<C> addHeader(CharSequence name, Object value) {
        if (state.allowUpdate()) {
            state.headers.headers().add(name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addCookie(Cookie cookie) {
        if (state.allowUpdate()) {
            state.headers.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addDateHeader(CharSequence name, Date value) {
        if (state.allowUpdate()) {
            state.headers.headers().add(name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addDateHeader(CharSequence name, Iterable<Date> values) {
        if (state.allowUpdate()) {
            for (Date value : values) {
                state.headers.headers().add(name, value);
            }
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addHeader(CharSequence name, Iterable<Object> values) {
        if (state.allowUpdate()) {
            state.headers.headers().add(name, values);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setDateHeader(CharSequence name, Date value) {
        if (state.allowUpdate()) {
            state.headers.headers().set(name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setHeader(CharSequence name, Object value) {
        if (state.allowUpdate()) {
            state.headers.headers().set(name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setDateHeader(CharSequence name, Iterable<Date> values) {
        if (state.allowUpdate()) {
            for (Date value : values) {
                state.headers.headers().set(name, value);
            }
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setHeader(CharSequence name, Iterable<Object> values) {
        if (state.allowUpdate()) {
            state.headers.headers().set(name, values);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> removeHeader(CharSequence name) {
        if (state.allowUpdate()) {
            state.headers.headers().remove(name);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setStatus(HttpResponseStatus status) {
        if (state.allowUpdate()) {
            state.headers.setStatus(status);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setTransferEncodingChunked() {
        if (state.allowUpdate()) {
            HttpUtil.setTransferEncodingChunked(state.headers, true);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> flushOnlyOnReadComplete() {
        // Does not need to be guarded by allowUpdate() as flush semantics can be changed anytime.
        state.connection.unsafeNettyChannel().attr(ChannelOperations.FLUSH_ONLY_ON_READ_COMPLETE).set(true);
        return this;
    }

    @Override
    public ResponseContentWriter<C> sendHeaders() {
        return state.sendHeaders();
    }

    @Override
    public HttpServerResponse<ServerSentEvent> transformToServerSentEvents() {
        markAwarePipeline().addAfter(HttpHandlerNames.HttpServerEncoder.getName(),
                                            HttpHandlerNames.SseServerCodec.getName(),
                                            new ServerSentEventEncoder());
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> transformContent(AllocatingTransformer<CC, C> transformer) {
        @SuppressWarnings("unchecked")
        Connection transformedC = state.connection.transformWrite(transformer);
        return new HttpServerResponseImpl<>(new State<CC>(state, transformedC));
    }

    @Override
    public WebSocketHandshaker acceptWebSocketUpgrade(WebSocketHandler handler) {
        return WebSocketHandshaker.isUpgradeRequested(state.request)
                ? WebSocketHandshaker.newHandshaker(state.request, this, handler)
                : WebSocketHandshaker.newErrorHandshaker(new IllegalStateException("WebSocket upgrade was not requested."));
    }

    @Override
    public Observable<Void> dispose() {
        return Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                return (state.allowUpdate() ? write(Observable.<C>empty()) : Observable.<Void>empty())
                        .doOnSubscribe(new Action0() {
                            @Override
                            public void call() {
                                state.connection
                                        .getResettableChannelPipeline()
                                        .reset();
                            }
                        });
            }
        });
    }

    @Override
    public Channel unsafeNettyChannel() {
        return state.connection.unsafeNettyChannel();
    }

    @Override
    public Connection<?, ?> unsafeConnection() {
        return state.connection;
    }

    @Override
    public ResponseContentWriter<C> write(Observable<C> msgs) {
        return state.sendHeaders().write(msgs);
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource, Func0<T> trailerFactory,
                                                              Func2<T, C, T> trailerMutator) {
        return state.sendHeaders().write(contentSource, trailerFactory, trailerMutator);
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource, Func0<T> trailerFactory,
                                                              Func2<T, C, T> trailerMutator,
                                                              Func1<C, Boolean> flushSelector) {
        return state.sendHeaders().write(contentSource, trailerFactory, trailerMutator, flushSelector);
    }

    @Override
    public ResponseContentWriter<C> write(Observable<C> msgs, Func1<C, Boolean> flushSelector) {
        return state.sendHeaders().write(msgs, flushSelector);
    }

    @Override
    public ResponseContentWriter<C> writeAndFlushOnEach(Observable<C> msgs) {
        return state.sendHeaders().writeAndFlushOnEach(msgs);
    }

    @Override
    public ResponseContentWriter<C> writeString(Observable<String> msgs) {
        return state.sendHeaders().writeString(msgs);
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                    Func0<T> trailerFactory,
                                                                    Func2<T, String, T> trailerMutator) {
        return state.sendHeaders().writeString(contentSource, trailerFactory, trailerMutator);
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                    Func0<T> trailerFactory,
                                                                    Func2<T, String, T> trailerMutator,
                                                                    Func1<String, Boolean> flushSelector) {
        return state.sendHeaders().writeString(contentSource, trailerFactory, trailerMutator, flushSelector);
    }

    @Override
    public ResponseContentWriter<C> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector) {
        return state.sendHeaders().writeString(msgs, flushSelector);
    }

    @Override
    public ResponseContentWriter<C> writeStringAndFlushOnEach(Observable<String> msgs) {
        return state.sendHeaders().writeStringAndFlushOnEach(msgs);
    }

    @Override
    public ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs) {
        return state.sendHeaders().writeBytes(msgs);
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                   Func0<T> trailerFactory,
                                                                   Func2<T, byte[], T> trailerMutator) {
        return state.sendHeaders().writeBytes(contentSource, trailerFactory, trailerMutator);
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                   Func0<T> trailerFactory,
                                                                   Func2<T, byte[], T> trailerMutator,
                                                                   Func1<byte[], Boolean> flushSelector) {
        return state.sendHeaders().writeBytes(contentSource, trailerFactory, trailerMutator, flushSelector);
    }

    @Override
    public ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector) {
        return state.sendHeaders().writeBytes(msgs, flushSelector);
    }

    @Override
    public ResponseContentWriter<C> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
        return state.sendHeaders().writeBytesAndFlushOnEach(msgs);
    }

    public static <T> HttpServerResponse<T> create(HttpServerRequest<?> request,
                                                   @SuppressWarnings("rawtypes") Connection connection,
                                                   HttpResponse headers) {
        final State<T> newState = new State<>(headers, connection, request);
        return new HttpServerResponseImpl<>(newState);
    }

    @SuppressWarnings("unchecked")
    private <CC> HttpServerResponse<CC> _cast() {
        return (HttpServerResponse<CC>) this;
    }

    private MarkAwarePipeline markAwarePipeline() {
        return state.connection.getResettableChannelPipeline().markIfNotYetMarked();
    }

    private static class State<T> {

        private final HttpResponse headers;

        @SuppressWarnings("rawtypes")
        private final Connection connection;
        private final HttpServerRequest<?> request;
        /*This links the headers sent dynamic state from one response to a child response
        (created via a mutation method). If it is a simple boolean, then a copy of state will just lead to a copy by
        value and not reference.*/
        private final HeaderSentStateHolder sentStateHolder;

        private State(HttpResponse headers, @SuppressWarnings("rawtypes") Connection connection, HttpServerRequest<?> request) {
            this.headers = headers;
            this.connection = connection;
            this.request = request;
            this.sentStateHolder = new HeaderSentStateHolder();
        }

        public State(State<?> state, Connection connection) {
            this.headers = state.headers;
            this.request = state.request;
            this.sentStateHolder = state.sentStateHolder;
            this.connection = connection;
        }

        private boolean allowUpdate() {
            return !sentStateHolder.headersSent;
        }

        public ResponseContentWriter<T> sendHeaders() {
            if (allowUpdate()) {
                sentStateHolder.headersSent = true;
                return new ContentWriterImpl<>(connection, headers);
            }

            return new FailedContentWriter<>();
        }

    }

    private static final class HeaderSentStateHolder implements Func0 {

        private boolean headersSent = false;

        @Override
        public Object call() {
            return headersSent;
        }
    }
}
