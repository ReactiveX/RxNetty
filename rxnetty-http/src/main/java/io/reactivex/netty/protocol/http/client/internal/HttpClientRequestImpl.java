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
package io.reactivex.netty.protocol.http.client.internal;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.netty.channel.AllocatingTransformer;
import io.reactivex.netty.channel.AppendTransformerEvent;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.events.EventAttributeKeys;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.internal.VoidToAnythingCast;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.protocol.http.internal.OperatorTrailer;
import io.reactivex.netty.protocol.http.ws.client.internal.WebSocketRequestImpl;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public final class HttpClientRequestImpl<I, O> extends HttpClientRequest<I, O> {

    public static final int NO_REDIRECTS = -1;

    private final List<AppendTransformerEvent> immutableTransformers;
    private final List<Transformer> immutableResponseTransformers;
    private final RawRequest<I, O> rawRequest;
    private final TcpClient<?, HttpClientResponse<O>> client;
    private final Func1<I, Boolean> flushOnEachSelector = new Func1<I, Boolean>() {
        @Override
        public Boolean call(I next) {
            return true;
        }
    };

    private HttpClientRequestImpl(final RawRequest<I, O> rawRequest, final TcpClient<?, HttpClientResponse<O>> client,
                                  List<AppendTransformerEvent> immutableTransformers,
                                  List<Transformer> immutableResponseTransformers) {
        super(new OnSubscribeFuncImpl<>(client, rawRequest, immutableResponseTransformers, immutableTransformers));
        this.rawRequest = rawRequest;
        this.client = client;
        this.immutableTransformers = immutableTransformers;
        this.immutableResponseTransformers = immutableResponseTransformers;
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
    public HttpClientRequestImpl<I, O> readTimeOut(int timeOut, TimeUnit timeUnit) {
        return _copy(client.readTimeOut(timeOut, timeUnit));
    }

    @Override
    public HttpClientRequestImpl<I, O> followRedirects(int maxRedirects) {
        final Redirector<I, O> redirector = new Redirector<>(maxRedirects, client);
        HttpClientRequestImpl<I, O> toReturn = _copy(client, rawRequest.followRedirect(redirector));
        redirector.setOriginalRequest(toReturn.rawRequest);
        return toReturn;
    }

    @Override
    public HttpClientRequestImpl<I, O> followRedirects(boolean follow) {
        return follow ? followRedirects(Redirector.DEFAULT_MAX_REDIRECTS) : followRedirects(NO_REDIRECTS);
    }

    @Override
    public HttpClientRequestImpl<I, O> setMethod(HttpMethod method) {
        return _copy(client, rawRequest.setMethod(method));
    }

    @Override
    public HttpClientRequestImpl<I, O> setUri(String newUri) {
        return _copy(client, rawRequest.setUri(newUri));
    }

    @Override
    public HttpClientRequestImpl<I, O> addHeader(CharSequence name, Object value) {
        return _copy(client, rawRequest.addHeader(name, value));
    }

    @Override
    public HttpClientRequest<I, O> addHeaders(Map<? extends CharSequence, ? extends Iterable<Object>> headers) {
        return _copy(client, rawRequest.addHeaders(headers));
    }

    @Override
    public HttpClientRequestImpl<I, O> addCookie(Cookie cookie) {
        return _copy(client, rawRequest.addCookie(cookie));
    }

    @Override
    public HttpClientRequestImpl<I, O> addDateHeader(CharSequence name, Date value) {
        return _copy(client, rawRequest.addDateHeader(name, value));
    }

    @Override
    public HttpClientRequestImpl<I, O> addDateHeader(CharSequence name, Iterable<Date> values) {
        return _copy(client, rawRequest.addDateHeader(name, values));
    }

    @Override
    public HttpClientRequestImpl<I, O> addHeaderValues(CharSequence name, Iterable<Object> values) {
        return _copy(client, rawRequest.addHeaderValues(name, values));
    }

    @Override
    public HttpClientRequestImpl<I, O> setDateHeader(CharSequence name, Date value) {
        return _copy(client, rawRequest.setDateHeader(name, value));
    }

    @Override
    public HttpClientRequestImpl<I, O> setHeader(CharSequence name, Object value) {
        return _copy(client, rawRequest.setHeader(name, value));
    }

    @Override
    public HttpClientRequest<I, O> setHeaders(Map<? extends CharSequence, ? extends Iterable<Object>> headers) {
        return _copy(client, rawRequest.setHeaders(headers));
    }

    @Override
    public HttpClientRequestImpl<I, O> setDateHeader(CharSequence name, Iterable<Date> values) {
        return _copy(client, rawRequest.setDateHeader(name, values));
    }

    @Override
    public HttpClientRequestImpl<I, O> setHeaderValues(CharSequence name, Iterable<Object> values) {
        return _copy(client, rawRequest.setHeaderValues(name, values));
    }

    @Override
    public HttpClientRequestImpl<I, O> removeHeader(CharSequence name) {
        return _copy(client, rawRequest.removeHeader(name));
    }

    @Override
    public HttpClientRequestImpl<I, O> setKeepAlive(boolean keepAlive) {
        return _copy(client, rawRequest.setKeepAlive(keepAlive));
    }

    @Override
    public HttpClientRequestImpl<I, O> setTransferEncodingChunked() {
        return _copy(client, rawRequest.setTransferEncodingChunked());
    }

    @Override
    public <II> HttpClientRequestImpl<II, O> transformContent(AllocatingTransformer<II, I> transformer) {
        final List<AppendTransformerEvent> newTransformers = new ArrayList<>(immutableTransformers);
        @SuppressWarnings("unchecked")
        AppendTransformerEvent e = new AppendTransformerEvent(transformer);
        newTransformers.add(e);
        @SuppressWarnings("unchecked")
        RawRequest<II, O> cast = (RawRequest<II, O>) this.rawRequest;
        return new HttpClientRequestImpl<>(cast, client, newTransformers, immutableResponseTransformers);
    }

    @Override
    public <OO> HttpClientRequestImpl<I, OO> transformResponseContent(Transformer<O, OO> transformer) {
        final List<Transformer> newTransformers = new ArrayList<>(immutableResponseTransformers);
        newTransformers.add(transformer);
        @SuppressWarnings("unchecked")
        RawRequest<I, OO> cast = (RawRequest<I, OO>) this.rawRequest;
        TcpClient rawClient = client;
        @SuppressWarnings("unchecked")
        TcpClient<?, HttpClientResponse<OO>> _client = (TcpClient<?, HttpClientResponse<OO>>)rawClient;
        return new HttpClientRequestImpl<>(cast, _client, immutableTransformers, newTransformers);
    }

    @Override
    public WebSocketRequestImpl<O> requestWebSocketUpgrade() {
        return WebSocketRequestImpl.createNew(this);
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return rawRequest.getHeaders().headers().contains(name);
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
    public Iterator<Entry<CharSequence, CharSequence>> headerIterator() {
        return rawRequest.getHeaders().headers().iteratorCharSequence();
    }

    @Override
    public Set<String> getHeaderNames() {
        return rawRequest.getHeaders().headers().names();
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

    public static <I, O> HttpClientRequestImpl<I, O> create(final HttpVersion version, final HttpMethod httpMethod,
                                                        final String uri,
                                                        final TcpClient<?, HttpClientResponse<O>> client,
                                                        int maxRedirects) {
        Redirector<I, O> redirector = NO_REDIRECTS == maxRedirects
                                                                ? null
                                                                : new Redirector<I, O>(maxRedirects, client
        );

        final RawRequest<I, O> rawRequest = RawRequest.create(version, httpMethod, uri, redirector);

        if (null != redirector) {
            redirector.setOriginalRequest(rawRequest);
        }

        return create(rawRequest, client);
    }

    public static <I, O> HttpClientRequestImpl<I, O> create(final HttpVersion version, final HttpMethod httpMethod,
                                                        final String uri,
                                                        final TcpClient<?, HttpClientResponse<O>> client) {
        return create(version, httpMethod, uri, client, NO_REDIRECTS);
    }

    public static <I, O> HttpClientRequestImpl<I, O> create(final RawRequest<I, O> rawRequest,
                                                        final TcpClient<?, HttpClientResponse<O>> client) {
        return new HttpClientRequestImpl<>(rawRequest, client, Collections.<AppendTransformerEvent>emptyList(),
                                           Collections.<Transformer>emptyList());
    }

    public TcpClient<?, HttpClientResponse<O>> getClient() {
        return client;
    }

    @SuppressWarnings("unchecked")
    private <II, OO> HttpClientRequestImpl<II, OO> _copy(TcpClient<?, HttpClientResponse<OO>> c) {
        return _copy(c, (RawRequest<II, OO>)rawRequest);
    }

    @SuppressWarnings("unchecked")
    private <II, OO> HttpClientRequestImpl<II, OO> _copy(TcpClient<?, HttpClientResponse<OO>> c,
                                                     RawRequest<II, OO> rawRequest) {
        return new HttpClientRequestImpl<>(rawRequest, c, immutableTransformers, immutableResponseTransformers);
    }

    @SuppressWarnings("rawtypes")
    private Observable<HttpClientResponse<O>> _writeContentRaw(Observable rawContent, boolean hasTrailers) {
        return _writeContentRaw(rawContent, null, hasTrailers);
    }

    @SuppressWarnings("rawtypes")
    private Observable<HttpClientResponse<O>> _writeContentRaw(Observable rawContent,
                                                               Func1<?, Boolean> flushSelector, boolean hasTrailers) {
        final RawRequest<I, O> r = RawRequest.create(rawRequest.getHeaders(), rawContent, flushSelector, hasTrailers,
                                                     rawRequest.getRedirector());
        return new HttpClientRequestImpl<>(r, client, immutableTransformers, immutableResponseTransformers);
    }

    public RawRequest<I, O> unsafeRawRequest() {
        return rawRequest;
    }

    private static class OnSubscribeFuncImpl<I, O> implements OnSubscribe<HttpClientResponse<O>> {
        @SuppressWarnings("rawtypes")
        private final Observable source;
        private final TcpClient<?, HttpClientResponse<O>> client;

        public OnSubscribeFuncImpl(final TcpClient<?, HttpClientResponse<O>> client, RawRequest<I, O> rawRequest,
                                   List<Transformer> responseTransformers,
                                   List<AppendTransformerEvent> requestTransformers) {
            this.client = client;
            ConnToResponseFunc<I, O> connToResponseFunc = new ConnToResponseFunc<>(rawRequest, responseTransformers,
                                                                                   requestTransformers);
            Observable<HttpClientResponse<O>> source = this.client.createConnectionRequest()
                                                                  .take(1)
                                                                  .switchMap(connToResponseFunc);

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

        private final RawRequest<I, O> rawRequest;
        private List<Transformer> responseTransformers;
        private List<AppendTransformerEvent> requestTransformers;

        public ConnToResponseFunc(RawRequest<I, O> rawRequest, List<Transformer> responseTransformers,
                                  List<AppendTransformerEvent> requestTransformers) {
            this.rawRequest = rawRequest;
            this.responseTransformers = responseTransformers;
            this.requestTransformers = requestTransformers;
        }

        @Override
        public Observable<HttpClientResponse<O>> call(final Connection<HttpClientResponse<O>, ?> conn) {
            for (AppendTransformerEvent requestTransformer : requestTransformers) {
                conn.unsafeNettyChannel().pipeline().fireUserEventTriggered(requestTransformer);
            }

            final Observable<HttpClientResponse<O>> input = conn.getInput();

            final HttpClientEventsListener eventsListener =
                    conn.unsafeNettyChannel().attr(HttpChannelProvider.HTTP_CLIENT_EVENT_LISTENER).get();
            final EventPublisher eventPublisher =
                    conn.unsafeNettyChannel().attr(EventAttributeKeys.EVENT_PUBLISHER).get();

            return writeRequest(conn).lift(new RequestWriteMetricsOperator(eventsListener, eventPublisher))
                                     .map(new VoidToAnythingCast<HttpClientResponse<O>>())
                                     .ignoreElements()
                                     .concatWith(input.take(1))
                                     .map(new Func1<HttpClientResponse<O>, HttpClientResponse<O>>() {
                                         @SuppressWarnings("unchecked")
                                         @Override
                                         public HttpClientResponse<O> call(HttpClientResponse<O> r) {
                                             HttpClientResponse rp = HttpClientResponseImpl.newInstance(r, conn);
                                             for (Transformer transformer : responseTransformers) {
                                                 rp = rp.transformContent(transformer);
                                             }
                                             return (HttpClientResponse<O>) rp;
                                         }
                                     });
        }

        @SuppressWarnings("unchecked")
        protected Observable<Void> writeRequest(Connection<HttpClientResponse<O>, ?> conn) {
            return conn.write(rawRequest.asObservable(conn));
        }
    }

    private static class RequestWriteMetricsOperator implements Operator<Void, Void> {

        private final EventPublisher eventPublisher;
        private final HttpClientEventsListener eventsListener;

        public RequestWriteMetricsOperator(HttpClientEventsListener eventsListener, EventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            this.eventsListener = eventsListener;
        }

        @Override
        public Subscriber<? super Void> call(final Subscriber<? super Void> o) {
            final long startTimeNanos = eventPublisher.publishingEnabled() ? Clock.newStartTimeNanos() : -1;
            if (eventPublisher.publishingEnabled()) {
                eventsListener.onRequestSubmitted();
            }
            return new Subscriber<Void>(o) {
                @Override
                public void onCompleted() {
                    if (eventPublisher.publishingEnabled()) {
                        eventsListener.onRequestWriteComplete(Clock.onEndNanos(startTimeNanos), NANOSECONDS);
                    }
                    o.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    if (eventPublisher.publishingEnabled()) {
                        eventsListener.onRequestWriteFailed(Clock.onEndNanos(startTimeNanos), NANOSECONDS, e);
                    }
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
