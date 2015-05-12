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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.util.concurrent.TimeUnit;

public class HttpClientImpl<I, O> extends HttpClient<I, O> {

    private final TcpClient<?, HttpClientResponse<O>> client;

    protected HttpClientImpl(TcpClient<?, HttpClientResponse<O>> client) {
        this.client = client;
    }

    @Override
    public HttpClientRequest<I, O> createGet(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.GET, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createPost(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.POST, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createPut(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.PUT, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createDelete(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.DELETE, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createHead(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.HEAD, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createOptions(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.OPTIONS, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createPatch(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.PATCH, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createTrace(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.TRACE, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createConnect(String uri) {
        return HttpClientRequestImpl.create(HttpMethod.CONNECT, uri, client);
    }

    @Override
    public HttpClientRequest<I, O> createRequest(HttpMethod method, String uri) {
        return HttpClientRequestImpl.create(method, uri, client);
    }

    @Override
    public HttpClient<I, O> readTimeOut(int timeOut, TimeUnit timeUnit) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> followRedirects(int maxRedirects) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> followRedirects(boolean follow) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <T> HttpClient<I, O> channelOption(ChannelOption<T> option, T value) {
        return _copy(client.channelOption(option, value));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerFirst(name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerFirst(group, name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerLast(name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerLast(group, name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerBefore(baseName, name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerBefore(group, baseName, name,
                                                                                  handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerAfter(baseName, name, handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerAfter(group, baseName, name,
                                                                                 handlerFactory)));
    }

    @Override
    public <II, OO> HttpClient<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return _copy(HttpClientImpl.<OO>castClient(client.pipelineConfigurator(pipelineConfigurator)));
    }

    @Override
    public HttpClient<I, O> maxConnections(int maxConnections) {
        return _copy(client.maxConnections(maxConnections));
    }

    @Override
    public HttpClient<I, O> idleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis) {
        return _copy(client.idleConnectionsTimeoutMillis(idleConnectionsTimeoutMillis));
    }

    @Override
    public HttpClient<I, O> connectionPoolLimitStrategy(PoolLimitDeterminationStrategy limitDeterminationStrategy) {
        return _copy(client.connectionPoolLimitStrategy(limitDeterminationStrategy));
    }

    @Override
    public HttpClient<I, O> idleConnectionCleanupTimer(Observable<Long> idleConnectionCleanupTimer) {
        return _copy(client.idleConnectionCleanupTimer(idleConnectionCleanupTimer));
    }

    @Override
    public HttpClient<I, O> noIdleConnectionCleanup() {
        return _copy(client.noIdleConnectionCleanup());
    }

    @Override
    public HttpClient<I, O> noConnectionPooling() {
        return _copy(client.noConnectionPooling());
    }

    @Override
    public HttpClient<I, O> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return _copy(client.secure(sslEngineFactory));
    }

    @Override
    public HttpClient<I, O> secure(SSLEngine sslEngine) {
        return _copy(client.secure(sslEngine));
    }

    @Override
    public HttpClient<I, O> secure(SslCodec sslCodec) {
        return _copy(client.secure(sslCodec));
    }

    @Override
    public HttpClient<I, O> unsafeSecure() {
        return _copy(client.unsafeSecure());
    }

    @Override
    public HttpClient<I, O> enableWireLogging(LogLevel wireLoggingLevel) {
        return _copy(client.enableWireLogging(wireLoggingLevel));
    }

    @Override
    public Subscription subscribe(MetricEventsListener<? extends ClientMetricsEvent<?>> listener) {
        return client.subscribe(listener);
    }

    public static HttpClient<ByteBuf, ByteBuf> create(TcpClient<ByteBuf, ByteBuf> tcpClient) {
        return new HttpClientImpl<>(
                tcpClient.<Object, HttpClientResponse<ByteBuf>>pipelineConfigurator(new Action1<ChannelPipeline>() {
                    @Override
                    public void call(ChannelPipeline pipeline) {
                        // TODO: Fix events subject
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpClientToConnectionBridge<>(new MetricEventsSubject<ClientMetricsEvent<?>>()));
                    }
                }));
    }

    public static HttpClient<ByteBuf, ByteBuf> unsafeCreate(TcpClient<ByteBuf, ByteBuf> tcpClient) {
        return new HttpClientImpl<>(
                tcpClient.<Object, HttpClientResponse<ByteBuf>>pipelineConfigurator(new Action1<ChannelPipeline>() {
                    @Override
                    public void call(ChannelPipeline pipeline) {
                        // TODO: Fix events subject
                        pipeline.addLast(new HttpClientToConnectionBridge<>(new MetricEventsSubject<ClientMetricsEvent<?>>()));
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private static <OO> TcpClient<?, HttpClientResponse<OO>> castClient(TcpClient<?, ?> rawTypes) {
        return (TcpClient<?, HttpClientResponse<OO>>) rawTypes;
    }

    private static <II, OO> HttpClient<II, OO> _copy(TcpClient<?, HttpClientResponse<OO>> newClient) {
        return new HttpClientImpl<>(newClient);
    }
}
