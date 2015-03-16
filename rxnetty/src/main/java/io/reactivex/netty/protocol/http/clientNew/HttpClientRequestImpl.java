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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.internal.OperatorTrailer;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public final class HttpClientRequestImpl<I, O> extends HttpClientRequest<I, O> {

    private final Request request;
    private final TcpClient<?, HttpClientResponse<O>> client;

    private HttpClientRequestImpl(final Request request,
                                  final TcpClient<?, HttpClientResponse<O>> client) {
        super(new OnSubscribeFuncImpl<>(client, request));
        this.request = request;
        this.client = client;
    }

    @Override
    public Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable);
    }

    @Override
    public Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(rawObservable);
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource,
                                                                              final Func0<T> trailerFactory,
                                                                              final Func2<T, I, T> trailerMutator) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator));
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource,
                                                                                    Func0<T> trailerFactory,
                                                                                    Func2<T, String, T> trailerMutator) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator));
    }

    @Override
    public <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource,
                                                                                   Func0<T> trailerFactory,
                                                                                   Func2<T, byte[], T> trailerMutator) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return _writeContentRaw(OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator));
    }

    @Override
    public HttpClientRequest<I, O> readTimeOut(int timeOut, TimeUnit timeUnit) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> followRedirects(int maxRedirects) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> followRedirects(boolean follow) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> addHeader(CharSequence name, Object value) {
        return new HttpClientRequestImpl<>(request.addHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> addCookie(Cookie cookie) {
        return new HttpClientRequestImpl<>(request.addCookie(cookie), client);
    }

    @Override
    public HttpClientRequest<I, O> addDateHeader(CharSequence name, Date value) {
        return new HttpClientRequestImpl<>(request.addDateHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> addDateHeader(CharSequence name, Iterable<Date> values) {
        return new HttpClientRequestImpl<>(request.addDateHeader(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> addHeader(CharSequence name, Iterable<Object> values) {
        return new HttpClientRequestImpl<>(request.addHeader(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> setDateHeader(CharSequence name, Date value) {
        return new HttpClientRequestImpl<>(request.setDateHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> setHeader(CharSequence name, Object value) {
        return new HttpClientRequestImpl<>(request.setHeader(name, value), client);
    }

    @Override
    public HttpClientRequest<I, O> setDateHeader(CharSequence name, Iterable<Date> values) {
        return new HttpClientRequestImpl<>(request.setDateHeader(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> setHeader(CharSequence name, Iterable<Object> values) {
        return new HttpClientRequestImpl<>(request.setHeader(name, values), client);
    }

    @Override
    public HttpClientRequest<I, O> removeHeader(CharSequence name) {
        return new HttpClientRequestImpl<>(request.removeHeader(name), client);
    }

    @Override
    public HttpClientRequest<I, O> setKeepAlive(boolean keepAlive) {
        return new HttpClientRequestImpl<>(request.setKeepAlive(keepAlive), client);
    }

    @Override
    public HttpClientRequest<I, O> setTransferEncodingChunked() {
        return new HttpClientRequestImpl<>(request.setTransferEncodingChunked(), client);
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
    public boolean containsHeader(CharSequence name) {
        return request.headers.headers().contains(name);
    }

    @Override
    public HttpClientRequestUpdater<I, O> newUpdater() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public boolean containsHeaderWithValue(CharSequence name, CharSequence value, boolean caseInsensitiveValueMatch) {
        return request.headers.headers().contains(name, value, caseInsensitiveValueMatch);
    }

    @Override
    public String getHeader(CharSequence name) {
        return request.headers.headers().get(name);
    }

    @Override
    public List<String> getAllHeaders(CharSequence name) {
        return request.headers.headers().getAll(name);
    }

    @Override
    public HttpVersion getHttpVersion() {
        return request.headers.protocolVersion();
    }

    @Override
    public HttpMethod getMethod() {
        return request.headers.method();
    }

    @Override
    public String getUri() {
        return request.headers.uri();
    }

    static <I, O> HttpClientRequest<I, O> create(final HttpMethod httpMethod, final String uri,
                                                 final TcpClient<?, HttpClientResponse<O>> client) {
        final Request request = Request.create(httpMethod, uri);
        return new HttpClientRequestImpl<I, O>(request, client);
    }

    @SuppressWarnings("unchecked")
    private static <OO> TcpClient<Request, HttpClientResponse<OO>> castClient(TcpClient<?, ?> rawTypes) {
        return (TcpClient<Request, HttpClientResponse<OO>>) rawTypes;
    }

    @SuppressWarnings("unchecked")
    private <II, OO> HttpClientRequest<II, OO> _copy(TcpClient<?, HttpClientResponse<OO>> c) {
        return new HttpClientRequestImpl<II, OO>(request, c);
    }

    @SuppressWarnings("rawtypes")
    private Observable<HttpClientResponse<O>> _writeContentRaw(Observable rawContent) {
        final Request r = Request.create(request.headers, rawContent);
        return new HttpClientRequestImpl<>(r, client);
    }

    /*package private, not a public contract*/ static class Request {

        private final HttpRequest headers;
        @SuppressWarnings("rawtypes")
        private final Observable content;
        @SuppressWarnings("rawtypes")
        private final Observable asObservable;

        @SuppressWarnings("rawtypes")
        Request(HttpRequest headers, Observable content) {
            this.headers = headers;
            this.content = content;
            asObservable = null != content ? Observable.<Object>just(headers).concatWith(content)
                                           : Observable.<Object>just(headers);
        }

        public Request addHeader(CharSequence name, Object value) {
            HttpRequest headersCopy = _copyHeaders();
            headersCopy.headers().add(name, value);
            return create(headersCopy, content);
        }

        public Request addHeader(CharSequence name, Iterable<Object> values) {
            HttpRequest headersCopy = _copyHeaders();
            headersCopy.headers().add(name, values);
            return create(headersCopy, content);
        }

        public Request addCookie(Cookie cookie) {
            String cookieHeader = ClientCookieEncoder.encode(cookie);
            return addHeader(HttpHeaders.Names.COOKIE, cookieHeader);
        }

        public Request addDateHeader(CharSequence name, Date value) {
            HttpRequest headersCopy = _copyHeaders();
            HttpHeaders.addDateHeader(headersCopy, name, value);
            return create(headersCopy, content);
        }

        public Request addDateHeader(CharSequence name, Iterable<Date> values) {
            HttpRequest headersCopy = _copyHeaders();
            for (Date value : values) {
                HttpHeaders.addDateHeader(headersCopy, name, value);
            }
            return create(headersCopy, content);
        }

        public Request setDateHeader(CharSequence name, Date value) {
            HttpRequest headersCopy = _copyHeaders();
            HttpHeaders.setDateHeader(headersCopy, name, value);
            return create(headersCopy, content);
        }

        public Request setHeader(CharSequence name, Object value) {
            HttpRequest headersCopy = _copyHeaders();
            headersCopy.headers().set(name, value);
            return create(headersCopy, content);
        }

        public Request setHeader(CharSequence name, Iterable<Date> values) {
            HttpRequest headersCopy = _copyHeaders();
            headersCopy.headers().set(name, values);
            return create(headersCopy, content);
        }

        public Request setDateHeader(CharSequence name, Iterable<Date> values) {
            HttpRequest headersCopy = _copyHeaders();
            for (Date value : values) {
                HttpHeaders.setDateHeader(headersCopy, name, value);
            }
            return create(headersCopy, content);
        }

        public Request setKeepAlive(boolean keepAlive) {
            HttpRequest headersCopy = _copyHeaders();
            HttpHeaders.setKeepAlive(headersCopy, keepAlive);
            return create(headersCopy, content);
        }

        public Request setTransferEncodingChunked() {
            HttpRequest headersCopy = _copyHeaders();
            HttpHeaders.setTransferEncodingChunked(headersCopy);
            return create(headersCopy, content);
        }

        public Request removeHeader(CharSequence name) {
            HttpRequest headersCopy = _copyHeaders();
            HttpHeaders.removeHeader(headersCopy, name);
            return create(headersCopy, content);
        }

        @SuppressWarnings("rawtypes")
        /*Package private, to be used by channel writer*/ Observable asObservable() {
            return asObservable;
        }

        private HttpRequest _copyHeaders() {
            final HttpRequest newHeaders = new DefaultHttpRequest(headers.protocolVersion(), headers.method(),
                                                                  headers.uri());
            // TODO: May be we can optimize this by not copying
            for (Entry<String, String> header : headers.headers()) {
                newHeaders.headers().set(header.getKey(), header.getValue());
            }
            return newHeaders;
        }

        private static Request create(HttpMethod httpMethod, String uri) {
            final HttpRequest headers = new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpMethod, uri);
            return new Request(headers, null);
        }

        @SuppressWarnings("rawtypes")
        private static Request create(HttpRequest headers, Observable content) {
            return new Request(headers, content);
        }
    }

    private static class OnSubscribeFuncImpl<O> implements OnSubscribe<HttpClientResponse<O>> {

        @SuppressWarnings("rawtypes")
        private final TcpClient client;
        private final Request request;
        @SuppressWarnings("rawtypes")
        private final Observable source;

        public OnSubscribeFuncImpl(TcpClient<?, HttpClientResponse<O>> client, Request request) {
            this.client = client;
            this.request = request;
            @SuppressWarnings({"rawtypes", "unchecked"})
            Observable source = this.client.createConnectionRequest()
                                           .take(1)
                                           .switchMap(new Func1<Connection, Observable>() {
                                               @SuppressWarnings("rawtypes")
                                               @Override
                                               public Observable<HttpClientResponse> call(Connection c) {
                                                   return c.writeAndFlush(OnSubscribeFuncImpl.this.request.asObservable())
                                                           .ignoreElements()
                                                           .cast(HttpClientResponse.class)
                                                           .concatWith(c.getInput().take(1));
                                               }
                                           });
            this.source = source;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void call(Subscriber<? super HttpClientResponse<O>> subscriber) {
            @SuppressWarnings("rawtypes")
            final Subscriber rawSub = subscriber;
            source.subscribe(rawSub);
        }
    }
}
