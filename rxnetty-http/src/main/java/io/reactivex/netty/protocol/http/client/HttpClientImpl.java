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
package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.client.ChannelProviderFactory;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.protocol.http.HttpHandlerNames;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventPublisher;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.protocol.http.client.internal.HttpChannelProviderFactory;
import io.reactivex.netty.protocol.http.client.internal.HttpClientRequestImpl;
import io.reactivex.netty.protocol.http.client.internal.HttpClientToConnectionBridge;
import io.reactivex.netty.protocol.http.client.internal.Redirector;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.client.TcpClientImpl;
import io.reactivex.netty.ssl.SslCodec;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.protocol.http.client.internal.HttpClientRequestImpl.*;

public final class HttpClientImpl<I, O> extends HttpClient<I, O> {

    private final TcpClient<?, HttpClientResponse<O>> client;
    private int maxRedirects;
    private final HttpClientEventPublisher clientEventPublisher;

    private HttpClientImpl(TcpClient<?, HttpClientResponse<O>> client, int maxRedirects,
                           HttpClientEventPublisher clientEventPublisher) {
        this.client = client;
        this.maxRedirects = maxRedirects;
        this.clientEventPublisher = clientEventPublisher;
    }

    @Override
    public HttpClientRequest<I, O> createGet(String uri) {
        return createRequest(HttpMethod.GET, uri);
    }

    @Override
    public HttpClientRequest<I, O> createPost(String uri) {
        return createRequest(HttpMethod.POST, uri);
    }

    @Override
    public HttpClientRequest<I, O> createPut(String uri) {
        return createRequest(HttpMethod.PUT, uri);
    }

    @Override
    public HttpClientRequest<I, O> createDelete(String uri) {
        return createRequest(HttpMethod.DELETE, uri);
    }

    @Override
    public HttpClientRequest<I, O> createHead(String uri) {
        return createRequest(HttpMethod.HEAD, uri);
    }

    @Override
    public HttpClientRequest<I, O> createOptions(String uri) {
        return createRequest(HttpMethod.OPTIONS, uri);
    }

    @Override
    public HttpClientRequest<I, O> createPatch(String uri) {
        return createRequest(HttpMethod.PATCH, uri);
    }

    @Override
    public HttpClientRequest<I, O> createTrace(String uri) {
        return createRequest(HttpMethod.TRACE, uri);
    }

    @Override
    public HttpClientRequest<I, O> createConnect(String uri) {
        return createRequest(HttpMethod.CONNECT, uri);
    }

    @Override
    public HttpClientRequest<I, O> createRequest(HttpMethod method, String uri) {
        return createRequest(HttpVersion.HTTP_1_1, method, uri);
    }

    @Override
    public HttpClientRequest<I, O> createRequest(HttpVersion version, HttpMethod method, String uri) {
        return HttpClientRequestImpl.create(version, method, uri, client, maxRedirects);
    }

    @Override
    public HttpClient<I, O> readTimeOut(int timeOut, TimeUnit timeUnit) {
        return _copy(client.readTimeOut(timeOut, timeUnit));
    }

    @Override
    public HttpClient<I, O> followRedirects(int maxRedirects) {
        HttpClientImpl<I, O> toReturn = _copy(client);
        toReturn.maxRedirects = maxRedirects;
        return toReturn;
    }

    @Override
    public HttpClient<I, O> followRedirects(boolean follow) {
        HttpClientImpl<I, O> toReturn = _copy(client);
        toReturn.maxRedirects = follow ? Redirector.DEFAULT_MAX_REDIRECTS : NO_REDIRECTS;
        return toReturn;
    }

    @Override
    public <T> HttpClient<I, O> channelOption(ChannelOption<T> option, T value) {
        return _copy(client.channelOption(option, value));
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerFirst(name, handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerFirst(group, name, handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerLast(name, handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerLast(group, name, handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerBefore(baseName, name, handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerBefore(group, baseName, name,
                                                                                  handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerAfter(baseName, name, handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpClientImpl.<OO>castClient(client.addChannelHandlerAfter(group, baseName, name,
                                                                                 handlerFactory))
        );
    }

    @Override
    public <II, OO> HttpClient<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return _copy(HttpClientImpl.<OO>castClient(client.pipelineConfigurator(pipelineConfigurator))
        );
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
    public HttpClient<I, O> channelProvider(ChannelProviderFactory providerFactory) {
        return _copy(client.channelProvider(providerFactory));
    }

    @Override
    public Subscription subscribe(HttpClientEventsListener listener) {
        return clientEventPublisher.subscribe(listener);
    }

    public static HttpClient<ByteBuf, ByteBuf> create(ConnectionProviderFactory<ByteBuf, ByteBuf> providerFactory,
                                                      Observable<Host> hostStream) {
        return _newClient(TcpClientImpl.create(providerFactory, hostStream));
    }

    public static HttpClient<ByteBuf, ByteBuf> create(SocketAddress socketAddress) {
        return _newClient(TcpClientImpl.<ByteBuf, ByteBuf>create(socketAddress));
    }

    private static HttpClient<ByteBuf, ByteBuf> _newClient(TcpClient<ByteBuf, ByteBuf> tcpClient) {

        HttpClientEventPublisher clientEventPublisher = new HttpClientEventPublisher();

        TcpClient<Object, HttpClientResponse<ByteBuf>> client =
                tcpClient.<Object, HttpClientResponse<ByteBuf>>pipelineConfigurator(new Action1<ChannelPipeline>() {
                    @Override
                    public void call(ChannelPipeline pipeline) {
                        pipeline.addLast(HttpHandlerNames.HttpClientCodec.getName(), new HttpClientCodec());
                        pipeline.addLast(new HttpClientToConnectionBridge<>());
                    }
                }).channelProvider(new HttpChannelProviderFactory(clientEventPublisher));

        client.subscribe(clientEventPublisher);

        return new HttpClientImpl<>(client, NO_REDIRECTS, clientEventPublisher);
    }

    @SuppressWarnings("unchecked")
    private static <OO> TcpClient<?, HttpClientResponse<OO>> castClient(TcpClient<?, ?> rawTypes) {
        return (TcpClient<?, HttpClientResponse<OO>>) rawTypes;
    }

    private <II, OO> HttpClientImpl<II, OO> _copy(TcpClient<?, HttpClientResponse<OO>> newClient) {
        return new HttpClientImpl<>(newClient, maxRedirects, clientEventPublisher);
    }
}
