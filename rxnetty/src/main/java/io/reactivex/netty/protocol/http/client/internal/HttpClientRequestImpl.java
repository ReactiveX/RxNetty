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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.codec.HandlerNames;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.client.HttpClientMetricsEvent;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientRequestUpdater;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.internal.OperatorTrailer;
import io.reactivex.netty.protocol.http.internal.VoidToAnythingCast;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.sse.ServerSentEventDecoder;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class HttpClientRequestImpl<I, O> extends HttpClientRequest<I, O> {

    public static final int NO_REDIRECTS = -1;

    private final RawRequest<I, O> rawRequest;
    private final TcpClient<?, HttpClientResponse<O>> client;

    private final Func1<I, Boolean> flushOnEachSelector = new Func1<I, Boolean>() {
        @Override
        public Boolean call(I next) {
            return true;
        }
    };

    private HttpClientRequestImpl(final RawRequest<I, O> rawRequest, final TcpClient<?, HttpClientResponse<O>> client) {
        super(new OnSubscribeFuncImpl<>(client, rawRequest));
        this.rawRequest = rawRequest;
        this.client = client;
    }

    @Override
    public Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable, false);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeContentAndFlushOnEach(Observable<I> contentSource) {
        return writeContent(contentSource, flushOnEachSelector);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable, false);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable, false);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource,
                                                          Func1<I, Boolean> flushSelector) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable, flushSelector, false);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource,
                                                                Func1<String, Boolean> flushSelector) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable, flushSelector, false);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource,
                                                               Func1<byte[], Boolean> flushSelector) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable, flushSelector, false);
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource,
                                                                              final Func0<T> trailerFactory,
                                                                              final Func2<T, I, T> trailerMutator) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator), true);
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource,
                                                                                    Func0<T> trailerFactory,
                                                                                    Func2<T, String, T> trailerMutator) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator), true);
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource,
                                                                                   Func0<T> trailerFactory,
                                                                                   Func2<T, byte[], T> trailerMutator) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator), true);
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource,
                                                                                      Func0<T> trailerFactory,
                                                                                      Func2<T, I, T> trailerMutator,
                                                                                      Func1<I, Boolean> flushSelector) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator), flushSelector,
                                true);
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeStringContent(
            Observable<String> contentSource, Func0<T> trailerFactory, Func2<T, String, T> trailerMutator,
            Func1<String, Boolean> flushSelector) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator), flushSelector,
                                true);
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeBytesContent(
            Observable<byte[]> contentSource, Func0<T> trailerFactory, Func2<T, byte[], T> trailerMutator,
            Func1<byte[], Boolean> flushSelector) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator), flushSelector,
                                true);
    }

    @Override
    public HttpClientRequest<I, O> readTimeOut(int timeOut, TimeUnit timeUnit) {
        return _copy(client.readTimeOut(timeOut, timeUnit));
    }

    @Override
    public HttpClientRequest<I, O> followRedirects(int maxRedirects) {
        final Redirector<I, O> redirector = new Redirector<I, O>(maxRedirects, client);
        final RawRequest<I, O> newRawRequest = rawRequest.followRedirect(redirector);

        HttpClientRequestImpl<I, O> toReturn = new HttpClientRequestImpl<>(newRawRequest, client);
        redirector.setOriginalRequest(newRawRequest);
        return toReturn;
    }

    @Override
    public HttpClientRequest<I, O> followRedirects(boolean follow) {
        return follow ? followRedirects(Redirector.DEFAULT_MAX_REDIRECTS) : followRedirects(NO_REDIRECTS);
    }

    @Override
    public HttpClientRequest<I, O> setMethod(HttpMethod method) {
        return new HttpClientRequestImpl<>(rawRequest.setMethod(method), client);
    }

    @Override
    public HttpClientRequest<I, O> setUri(String newUri) {
        return new HttpClientRequestImpl<>(rawRequest.setUri(newUri), client);
    }

    @Override
    public HttpClientRequest<I, O> addHeader(CharSequence name, Object value) {
        return new HttpClientRequestImpl<>(rawRequest.addHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> addCookie(Cookie cookie) {
        return new HttpClientRequestImpl<>(rawRequest.addCookie(cookie), client);
    }

    @Override
    public HttpClientRequest<I, O> addDateHeader(CharSequence name, Date value) {
        return new HttpClientRequestImpl<>(rawRequest.addDateHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> addDateHeader(CharSequence name, Iterable<Date> values) {
        return new HttpClientRequestImpl<>(rawRequest.addDateHeader(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> addHeaderValues(CharSequence name, Iterable<Object> values) {
        return new HttpClientRequestImpl<>(rawRequest.addHeaderValues(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> setDateHeader(CharSequence name, Date value) {
        return new HttpClientRequestImpl<>(rawRequest.setDateHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> setHeader(CharSequence name, Object value) {
        return new HttpClientRequestImpl<>(rawRequest.setHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> setDateHeader(CharSequence name, Iterable<Date> values) {
        return new HttpClientRequestImpl<>(rawRequest.setDateHeader(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> setHeaderValues(CharSequence name, Iterable<Object> values) {
        return new HttpClientRequestImpl<>(rawRequest.setHeaderValues(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> removeHeader(CharSequence name) {
        return new HttpClientRequestImpl<>(rawRequest.removeHeader(name), client);
    }

    @Override
    public HttpClientRequest<I, O> setKeepAlive(boolean keepAlive) {
        return new HttpClientRequestImpl<>(rawRequest.setKeepAlive(keepAlive), client);
    }

    @Override
    public HttpClientRequest<I, O> setTransferEncodingChunked() {
        return new HttpClientRequestImpl<>(rawRequest.setTransferEncodingChunked(), client);
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerFirst(String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerFirst(name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerFirst(group, name,
                                                                                        handlerFactory)));
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerLast(name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                    Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerLast(group, name,
                                                                                       handlerFactory)));
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                      Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerBefore(baseName, name,
                                                                                         handlerFactory)));    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                      String name,
                                                                      Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerBefore(group, baseName, name,
                                                                                         handlerFactory)));
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerAfter(baseName, name,
                                                                                        handlerFactory)));
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                     String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.addChannelHandlerAfter(group, baseName, name,
                                                                                        handlerFactory)));
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> pipelineConfigurator(Action1<ChannelPipeline> configurator) {
        return _copy(HttpClientRequestImpl.<OO>castClient(client.pipelineConfigurator(configurator)));
    }

    @Override
    public HttpClientRequest<I, O> enableWireLogging(LogLevel wireLoggingLevel) {
        return _copy(client.enableWireLogging(wireLoggingLevel));
    }

    @Override
    public HttpClientRequest<O, ServerSentEvent> expectServerSentEvents() {
        return addChannelHandlerLast(HandlerNames.SseClientCodec.getName(), new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ServerSentEventDecoder();
            }
        });
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return rawRequest.getHeaders().headers().contains(name);
    }

    @Override
    public HttpClientRequestUpdater<I, O> newUpdater() {
        // TODO: Implement Updater
        return null;
    }

    @Override
    public boolean containsHeaderWithValue(CharSequence name, CharSequence value, boolean caseInsensitiveValueMatch) {
        return rawRequest.getHeaders().headers().contains(name, value, caseInsensitiveValueMatch);
    }

    @Override
    public String getHeader(CharSequence name) {
        return rawRequest.getHeaders().headers().get(name);
    }

    @Override
    public List<String> getAllHeaders(CharSequence name) {
        return rawRequest.getHeaders().headers().getAll(name);
    }

    @Override
    public HttpVersion getHttpVersion() {
        return rawRequest.getHeaders().protocolVersion();
    }

    @Override
    public HttpMethod getMethod() {
        return rawRequest.getHeaders().method();
    }

    @Override
    public String getUri() {
        return rawRequest.getHeaders().uri();
    }

    public static <I, O> HttpClientRequest<I, O> create(final HttpVersion version, final HttpMethod httpMethod,
                                                        final String uri,
                                                        final TcpClient<?, HttpClientResponse<O>> client,
                                                        int maxRedirects) {
        Redirector<I, O> redirector = NO_REDIRECTS == maxRedirects
                                                                ? null
                                                                : new Redirector<I, O>(maxRedirects, client);

        final RawRequest<I, O> rawRequest = RawRequest.create(version, httpMethod, uri, redirector);

        if (null != redirector) {
            redirector.setOriginalRequest(rawRequest);
        }

        final HttpClientRequestImpl<I, O> toReturn = new HttpClientRequestImpl<>(rawRequest, client);
        return toReturn;
    }

    public static <I, O> HttpClientRequest<I, O> create(final HttpVersion version, final HttpMethod httpMethod,
                                                        final String uri,
                                                        final TcpClient<?, HttpClientResponse<O>> client) {
        return create(version, httpMethod, uri, client, NO_REDIRECTS);
    }

    public static <I, O> HttpClientRequest<I, O> create(final RawRequest<I, O> rawRequest,
                                                        final TcpClient<?, HttpClientResponse<O>> client) {
        return new HttpClientRequestImpl<>(rawRequest, client);
    }

    @SuppressWarnings("unchecked")
    private static <OO> TcpClient<RawRequest<?, OO>, HttpClientResponse<OO>> castClient(TcpClient<?, ?> rawTypes) {
        return (TcpClient<RawRequest<?, OO>, HttpClientResponse<OO>>) rawTypes;
    }

    @SuppressWarnings("unchecked")
    private <II, OO> HttpClientRequest<II, OO> _copy(TcpClient<?, HttpClientResponse<OO>> c) {
        return new HttpClientRequestImpl<II, OO>((RawRequest<II, OO>) rawRequest, c);
    }

    @SuppressWarnings("rawtypes")
    private Observable<HttpClientResponse<O>> _writeContentRaw(Observable rawContent, boolean hasTrailers) {
        final RawRequest<I, O> r = RawRequest.create(rawRequest.getHeaders(), rawContent, hasTrailers,
                                               rawRequest.getRedirector());
        return new HttpClientRequestImpl<I, O>(r, client);
    }

    @SuppressWarnings("rawtypes")
    private Observable<HttpClientResponse<O>> _writeContentRaw(Observable rawContent,
                                                               Func1<?, Boolean> flushSelector, boolean hasTrailers) {
        final RawRequest<I, O> r = RawRequest
                .create(rawRequest.getHeaders(), rawContent, flushSelector, hasTrailers,
                        rawRequest.getRedirector());
        return new HttpClientRequestImpl<I, O>(r, client);
    }

    /*Visible for testing*/ RawRequest<I, O> getRawRequest() {
        return rawRequest;
    }

    /*Visible for testing*/ TcpClient<?, HttpClientResponse<O>> getTcpClient() {
        return client;
    }

    private static class OnSubscribeFuncImpl<I, O> implements OnSubscribe<HttpClientResponse<O>> {

        @SuppressWarnings("rawtypes")
        private final Observable source;
        private final TcpClient<?, HttpClientResponse<O>> client;

        public OnSubscribeFuncImpl(final TcpClient<?, HttpClientResponse<O>> client, RawRequest<I, O> rawRequest) {
            this.client = client;
            Observable<HttpClientResponse<O>> source = this.client.createConnectionRequest()
                                                                  .take(1)
                                                                  .switchMap(new ConnToResponseFunc<I, O>(client,
                                                                                                          rawRequest));

            if (null != rawRequest.getRedirector()) {
                source = source.switchMap(rawRequest.getRedirector());
            }

            this.source = source;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void call(Subscriber<? super HttpClientResponse<O>> subscriber) {
            @SuppressWarnings("rawtypes")
            final Subscriber rawSub = subscriber;
            source.unsafeSubscribe(rawSub);
        }

    }

    private static class ConnToResponseFunc<I, O>
            implements Func1<Connection<HttpClientResponse<O>, ?>, Observable<HttpClientResponse<O>>> {

        private final TcpClient<?, HttpClientResponse<O>> client;
        private final RawRequest<I, O> rawRequest;

        public ConnToResponseFunc(TcpClient<?, HttpClientResponse<O>> client, RawRequest<I, O> rawRequest) {
            this.client = client;
            this.rawRequest = rawRequest;
        }

        @Override
        public Observable<HttpClientResponse<O>> call(Connection<HttpClientResponse<O>, ?> conn) {
            final MetricEventsSubject<ClientMetricsEvent<?>> eSub = client.getEventsSubject();

            final Observable<HttpClientResponse<O>> input = conn.getInput();

            return writeRequest(conn).lift(new RequestWriteMetricsOperator(eSub))
                                     .map(new VoidToAnythingCast<HttpClientResponse<O>>())
                                     .ignoreElements()
                                     .concatWith(input.take(1));
        }

        @SuppressWarnings("unchecked")
        protected Observable<Void> writeRequest(Connection<HttpClientResponse<O>, ?> conn) {
            return conn.write(rawRequest.asObservable(conn.unsafeNettyChannel()));
        }
    }

    private static class RequestWriteMetricsOperator implements Operator<Void, Void> {

        private final MetricEventsSubject<ClientMetricsEvent<?>> eventSubject;

        public RequestWriteMetricsOperator(MetricEventsSubject<ClientMetricsEvent<?>> eventSubject) {
            this.eventSubject = eventSubject;
        }

        @Override
        public Subscriber<? super Void> call(final Subscriber<? super Void> o) {
            final long startTimeMillis = Clock.newStartTimeMillis();
            eventSubject.onEvent(HttpClientMetricsEvent.REQUEST_SUBMITTED);
            return new Subscriber<Void>(o) {
                @Override
                public void onCompleted() {
                    eventSubject.onEvent(HttpClientMetricsEvent.REQUEST_WRITE_COMPLETE,
                                         Clock.onEndMillis(startTimeMillis));
                    o.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    eventSubject.onEvent(HttpClientMetricsEvent.REQUEST_WRITE_FAILED,
                                         Clock.onEndMillis(startTimeMillis), e);
                    o.onError(e);
                }

                @Override
                public void onNext(Void aVoid) {
                    o.onNext(aVoid);
                }
            };
        }
    }
}
