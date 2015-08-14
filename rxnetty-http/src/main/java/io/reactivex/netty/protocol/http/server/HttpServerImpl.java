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
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.HttpHandlerNames;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventPublisher;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventsListener;
import io.reactivex.netty.protocol.http.ws.server.Ws7To13UpgradeHandler;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.reactivex.netty.events.Clock.*;
import static java.util.concurrent.TimeUnit.*;

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
    public HttpServer<I, O> enableWireLogging(LogLevel wireLoggingLevel) {
        return _copy(server.enableWireLogging(wireLoggingLevel), eventPublisher);
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
        server.start(new HttpServerConnectionHandler(requestHandler));
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

    private class HttpServerConnectionHandler implements ConnectionHandler<HttpServerRequest<I>, Object> {

        private final RequestHandler<I, O> requestHandler;

        private HttpServerConnectionHandler(RequestHandler<I, O> requestHandler) {
            this.requestHandler = requestHandler;
        }

        @Override
        public Observable<Void> handle(final Connection<HttpServerRequest<I>, Object> newConnection) {
            return newConnection.getInput()
                                .lift(new RequestHandlingOperator(newConnection));
        }

        private class RequestHandlingOperator implements Operator<Void, HttpServerRequest<I>> {

            private final Connection<HttpServerRequest<I>, Object> newConnection;

            public RequestHandlingOperator(Connection<HttpServerRequest<I>, Object> newConnection) {
                this.newConnection = newConnection;
            }

            @Override
            public Subscriber<? super HttpServerRequest<I>> call(final Subscriber<? super Void> subscriber) {

                return new Subscriber<HttpServerRequest<I>>() {

                    @Override
                    public void onStart() {
                        if (!newConnection.unsafeNettyChannel().config().isAutoRead()) {
                            request(1);
                        }
                    }

                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onNext(HttpServerRequest<I> request) {

                        final long startNanos = eventPublisher.publishingEnabled() ? newStartTimeNanos() : -1;

                        if (eventPublisher.publishingEnabled()) {
                            eventPublisher.onNewRequestReceived();
                        }

                        final HttpServerResponse<O> response = newResponse(request);

                        handleRequest(request, startNanos, response)
                                .doOnTerminate(new Action0() {
                                    @Override
                                    public void call() {
                                        if (!newConnection.unsafeNettyChannel()
                                                          .config()
                                                          .isAutoRead()) {
                                            request(1);
                                        }
                                    }
                                })
                                .ambWith(newConnection.closeListener())
                                .subscribe();
                    }
                };
            }

            @SuppressWarnings("unchecked")
            private Observable<Void> handleRequest(HttpServerRequest<I> request, final long startTimeNanos,
                                                   final HttpServerResponse<O> response) {
                Observable<Void> requestHandlingResult = null;
                try {

                    if (request.decoderResult().isSuccess()) {
                        requestHandlingResult = requestHandler.handle(request, response);
                    }

                    if(null == requestHandlingResult) {
                        /*If decoding failed an appropriate response status would have been set.
                          Otherwise, overwrite the status to 500*/
                        if (response.getStatus().equals(OK)) {
                            response.setStatus(INTERNAL_SERVER_ERROR);
                        }
                        requestHandlingResult = response.write(Observable.<O>empty());
                    }

                } catch (Throwable throwable) {
                    logger.error("Unexpected error while invoking HTTP user handler.", throwable);
                    /*If the headers are already written, then this will produce an error Observable.*/
                    requestHandlingResult = response.setStatus(INTERNAL_SERVER_ERROR)
                                                    .write(Observable.<O>empty());
                }

                if (eventPublisher.publishingEnabled()) {
                    requestHandlingResult = requestHandlingResult.lift(new Operator<Void, Void>() {
                        @Override
                        public Subscriber<? super Void> call(final Subscriber<? super Void> o) {

                            if (eventPublisher.publishingEnabled()) {
                                eventPublisher.onRequestHandlingStart(onEndNanos(startTimeNanos), NANOSECONDS);
                            }

                            return new Subscriber<Void>(o) {
                                @Override
                                public void onCompleted() {
                                    if (eventPublisher.publishingEnabled()) {
                                        eventPublisher.onRequestHandlingSuccess(onEndNanos(startTimeNanos),
                                                                                NANOSECONDS);
                                    }
                                    o.onCompleted();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (eventPublisher.publishingEnabled()) {
                                        eventPublisher.onRequestHandlingFailed(onEndNanos(startTimeNanos),
                                                                               NANOSECONDS, e);
                                    }
                                    logger.error("Unexpected error processing a request.", e);
                                    o.onError(e);
                                }

                                @Override
                                public void onNext(Void aVoid) {
                                    // No Op, its a void
                                }
                            };
                        }
                    });
                }

                return requestHandlingResult.onErrorResumeNext(new Func1<Throwable, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Throwable throwable) {
                        logger.error("Unexpected error while processing request.", throwable);
                        return response.setStatus(INTERNAL_SERVER_ERROR)
                                       .dispose()
                                       .concatWith(newConnection.close())
                                       .onErrorResumeNext(Observable.<Void>empty());// Ignore errors on cleanup
                    }
                }).concatWith(request.dispose()/*Dispose request at the end of processing to discard content if not read*/
                ).concatWith(response.dispose()/*Dispose response at the end of processing to cleanup*/);

            }

            private HttpServerResponse<O> newResponse(HttpServerRequest<I> request) {

                /*
                 * Server should send the highest version it is compatible with.
                 * http://tools.ietf.org/html/rfc2145#section-2.3
                 *
                 * unless overriden explicitly.
                 */
                final HttpVersion version = sendHttp10ResponseFor10Request ? request.getHttpVersion()
                                                                           : HttpVersion.HTTP_1_1;

                HttpResponse responseHeaders;
                if (request.decoderResult().isFailure()) {
                    // As per the spec, we should send 414/431 for URI too long and headers too long, but we do not have
                    // enough info to decide which kind of failure has caused this error here.
                    responseHeaders = new DefaultHttpResponse(version, REQUEST_HEADER_FIELDS_TOO_LARGE);
                    responseHeaders.headers()
                                   .set(CONNECTION, HttpHeaderValues.CLOSE)
                                   .set(CONTENT_LENGTH, 0);
                } else {
                    responseHeaders = new DefaultHttpResponse(version, OK);
                }
                HttpServerResponse<O> response = HttpServerResponseImpl.create(request, newConnection,
                                                                               responseHeaders);
                setConnectionHeader(request, response);
                return response;
            }

            private void setConnectionHeader(HttpServerRequest<I> request, HttpServerResponse<O> response) {
                if (request.isKeepAlive()) {
                    if (!request.getHttpVersion().isKeepAliveDefault()) {
                        // Avoid sending keep-alive header if keep alive is default.
                        // Issue: https://github.com/Netflix/RxNetty/issues/167
                        // This optimizes data transferred on the wire.

                        // Add keep alive header as per:
                        // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                        response.setHeader(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    }
                } else {
                    response.setHeader(CONNECTION, HttpHeaderValues.CLOSE);
                }
            }

        }
    }
}
