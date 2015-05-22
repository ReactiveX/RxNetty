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
package io.reactivex.netty.protocol.http.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ChannelOperations;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.MarkAwarePipeline;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.sse.ServerSentEventEncoder;
import io.reactivex.netty.protocol.tcp.internal.LoggingHandlerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class HttpServerResponseImpl<C> extends HttpServerResponse<C> {

    private final State<C> state;

    private HttpServerResponseImpl(final State<C> state) {
        super(new OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                state.sendHeaders().unsafeSubscribe(subscriber);
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
        return HttpHeaders.getHeader(state.headers, name, defaultValue);
    }

    @Override
    public List<String> getAllHeaderValues(CharSequence name) {
        return state.headers.headers().getAll(name);
    }

    @Override
    public Date getDateHeader(CharSequence name) throws ParseException {
        return HttpHeaders.getDateHeader(state.headers, name);
    }

    @Override
    public Date getDateHeader(CharSequence name, Date defaultValue) {
        return HttpHeaders.getDateHeader(state.headers, name, defaultValue);
    }

    @Override
    public int getIntHeader(CharSequence name) {
        return HttpHeaders.getIntHeader(state.headers, name);
    }

    @Override
    public int getIntHeader(CharSequence name, int defaultValue) {
        return HttpHeaders.getIntHeader(state.headers, name, defaultValue);
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
            state.headers.headers().add(Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addDateHeader(CharSequence name, Date value) {
        if (state.allowUpdate()) {
            HttpHeaders.addDateHeader(state.headers, name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> addDateHeader(CharSequence name, Iterable<Date> values) {
        if (state.allowUpdate()) {
            for (Date value : values) {
                HttpHeaders.addDateHeader(state.headers, name, value);
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
            HttpHeaders.setDateHeader(state.headers, name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setHeader(CharSequence name, Object value) {
        if (state.allowUpdate()) {
            HttpHeaders.setHeader(state.headers, name, value);
        }
        return this;
    }

    @Override
    public HttpServerResponse<C> setDateHeader(CharSequence name, Iterable<Date> values) {
        if (state.allowUpdate()) {
            for (Date value : values) {
                HttpHeaders.setDateHeader(state.headers, name, value);
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
            HttpHeaders.setTransferEncodingChunked(state.headers);
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
    public HttpServerResponse<ServerSentEvent> transformToServerSentEvents() {
        return addChannelHandlerAfter(HandlerNames.HttpServerEncoder.getName(), HandlerNames.SseServerCodec.getName(),
                                      new ServerSentEventEncoder());
    }

    @Override
    public HttpServerResponse<C> enableWireLogging(LogLevel wireLogginLevel) {
        return addChannelHandlerFirst(HandlerNames.WireLogging.getName(), LoggingHandlerFactory.get(wireLogginLevel));
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerFirst(String name, ChannelHandler handler) {
        markAwarePipeline().addFirst(name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                              ChannelHandler handler) {
        markAwarePipeline().addFirst(group, name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerLast(String name, ChannelHandler handler) {
        markAwarePipeline().addLast(name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                             ChannelHandler handler) {
        markAwarePipeline().addLast(group, name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerBefore(String baseName, String name, ChannelHandler handler) {
        markAwarePipeline().addBefore(baseName, name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                               ChannelHandler handler) {
        markAwarePipeline().addBefore(group, baseName, name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerAfter(String baseName, String name, ChannelHandler handler) {
        markAwarePipeline().addAfter(baseName, name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                              ChannelHandler handler) {
        markAwarePipeline().addAfter(group, baseName, name, handler);
        return _cast();
    }

    @Override
    public <CC> HttpServerResponse<CC> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        pipelineConfigurator.call(markAwarePipeline());
        return _cast();
    }

    @Override
    public Observable<Void> dispose() {
        return Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                return (state.allowUpdate()? write(Observable.<C>empty()) : Observable.<Void>empty())
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

    public static <T> HttpServerResponse<T> create(@SuppressWarnings("rawtypes") Connection connection,
                                                   HttpResponse headers) {
        final State<T> newState = new State<>(headers, connection);
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
        private boolean headersSent; /*Class is not thread safe*/

        private State(HttpResponse headers, @SuppressWarnings("rawtypes") Connection connection) {
            this.headers = headers;
            this.connection = connection;
        }

        private boolean allowUpdate() {
            return !headersSent;
        }

        public ResponseContentWriter<T> sendHeaders() {
            if (allowUpdate()) {
                headersSent = true;
                return new ContentWriterImpl<>(connection, headers);
            }

            return new FailedContentWriter<>();
        }
    }

    private static class FailedContentWriter<C> extends ResponseContentWriter<C> {

        private FailedContentWriter() {
            super(new OnSubscribe<Void>() {
                @Override
                public void call(Subscriber<? super Void> subscriber) {
                    subscriber.onError(new IllegalStateException("HTTP headers are already sent."));
                }
            });
        }

        @Override
        public ResponseContentWriter<C> write(Observable<C> msgs) {
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
        public ResponseContentWriter<C> write(Observable<C> msgs, Func1<C, Boolean> flushSelector) {
            return this;
        }

        @Override
        public ResponseContentWriter<C> writeAndFlushOnEach(Observable<C> msgs) {
            return this;
        }

        @Override
        public ResponseContentWriter<C> writeString(Observable<String> msgs) {
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
        public ResponseContentWriter<C> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector) {
            return this;
        }

        @Override
        public ResponseContentWriter<C> writeStringAndFlushOnEach(Observable<String> msgs) {
            return this;
        }

        @Override
        public ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs) {
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
        public ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector) {
            return this;
        }

        @Override
        public ResponseContentWriter<C> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
            return this;
        }
    }
}
