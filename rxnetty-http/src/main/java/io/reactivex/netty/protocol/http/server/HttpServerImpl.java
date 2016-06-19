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
 *
 */
package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.protocol.http.HttpHandlerNames;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventPublisher;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventsListener;
import io.reactivex.netty.protocol.http.ws.server.Ws7To13UpgradeHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.ssl.SslCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public final class HttpServerImpl<I, O> extends HttpServer<I, O> {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerImpl.class);

    private final TcpServer<HttpServerRequest<I>, Object> server;
    private final HttpServerEventPublisher eventPublisher;
    private boolean sendHttp10ResponseFor10Request;

    private HttpServerImpl(TcpServer<HttpServerRequest<I>, Object> server, HttpServerEventPublisher eventPublisher) {
        this.server = server;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public <T> HttpServer<I, O> channelOption(ChannelOption<T> option, T value) {
        return _copy(server.channelOption(option, value), eventPublisher);
    }

    @Override
    public <T> HttpServer<I, O> clientChannelOption(ChannelOption<T> option, T value) {
        return _copy(server.clientChannelOption(option, value), eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerFirst(name, handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerFirst(group, name, handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerLast(name, handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerLast(group, name, handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerBefore(baseName, name, handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerBefore(group, baseName, name,
                                                                                  handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerAfter(baseName, name, handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return _copy(HttpServerImpl.<II>castServer(server.addChannelHandlerAfter(group, baseName, name,
                                                                                 handlerFactory)),
                     eventPublisher);
    }

    @Override
    public <II, OO> HttpServer<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return _copy(HttpServerImpl.<II>castServer(server.pipelineConfigurator(pipelineConfigurator)),
                     eventPublisher);
    }

    @Override
    public HttpServer<I, O> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return _copy(server.secure(sslEngineFactory), eventPublisher);
    }

    @Override
    public HttpServer<I, O> secure(SSLEngine sslEngine) {
        return _copy(server.secure(sslEngine), eventPublisher);
    }

    @Override
    public HttpServer<I, O> secure(SslCodec sslCodec) {
        return _copy(server.secure(sslCodec), eventPublisher);
    }

    @Override
    public HttpServer<I, O> unsafeSecure() {
        return _copy(server.unsafeSecure(), eventPublisher);
    }

    @Override
    @Deprecated
    public HttpServer<I, O> enableWireLogging(LogLevel wireLoggingLevel) {
        return _copy(server.enableWireLogging(wireLoggingLevel), eventPublisher);
    }

    @Override
    public HttpServer<I, O> enableWireLogging(String name, LogLevel wireLoggingLevel) {
        return _copy(server.enableWireLogging(name, wireLoggingLevel), eventPublisher);
    }

    @Override
    public HttpServer<I, O> sendHttp10ResponseFor10Request(boolean sendHttp10ResponseFor10Request) {
        HttpServerImpl<I, O> toReturn = _copy(server, eventPublisher);
        toReturn.sendHttp10ResponseFor10Request = sendHttp10ResponseFor10Request;
        return toReturn;
    }

    @Override
    public int getServerPort() {
        return server.getServerPort();
    }

    @Override
    public SocketAddress getServerAddress() {
        return server.getServerAddress();
    }

    @Override
    public HttpServer<I, O> start(RequestHandler<I, O> requestHandler) {
        server.start(new HttpConnectionHandler<>(requestHandler, eventPublisher, sendHttp10ResponseFor10Request));
        return this;
    }

    @Override
    public void shutdown() {
        server.shutdown();
    }

    @Override
    public void awaitShutdown() {
        server.awaitShutdown();
    }

    @Override
    public void awaitShutdown(long duration, TimeUnit timeUnit) {
        server.awaitShutdown(duration, timeUnit);
    }

    static HttpServer<ByteBuf, ByteBuf> create(final TcpServer<ByteBuf, ByteBuf> tcpServer) {
        final HttpServerEventPublisher eventPublisher = new HttpServerEventPublisher(tcpServer.getEventPublisher());
        return new HttpServerImpl<>(
                tcpServer.<HttpServerRequest<ByteBuf>, Object>pipelineConfigurator(new Action1<ChannelPipeline>() {
                    @Override
                    public void call(ChannelPipeline pipeline) {
                        pipeline.addLast(HttpHandlerNames.HttpServerEncoder.getName(), new HttpResponseEncoder());
                        pipeline.addLast(HttpHandlerNames.HttpServerDecoder.getName(), new HttpRequestDecoder());
                        pipeline.addLast(HttpHandlerNames.WsServerUpgradeHandler.getName(), new Ws7To13UpgradeHandler());
                        pipeline.addLast(new HttpServerToConnectionBridge<>(eventPublisher));
                    }
                }), eventPublisher);
    }

    @SuppressWarnings("unchecked")
    private static <II> TcpServer<HttpServerRequest<II>, Object> castServer(TcpServer<?, ?> rawTypes) {
        return (TcpServer<HttpServerRequest<II>, Object>)rawTypes;
    }

    private static <II, OO> HttpServerImpl<II, OO> _copy(TcpServer<HttpServerRequest<II>, Object> newServer,
                                                         HttpServerEventPublisher oldEventPublisher) {
        return new HttpServerImpl<>(newServer, oldEventPublisher.copy(newServer.getEventPublisher()));
    }

    @Override
    public Subscription subscribe(HttpServerEventsListener listener) {
        final CompositeSubscription cs = new CompositeSubscription();
        cs.add(server.subscribe(listener));
        cs.add(eventPublisher.subscribe(listener));
        return cs;
    }
}
