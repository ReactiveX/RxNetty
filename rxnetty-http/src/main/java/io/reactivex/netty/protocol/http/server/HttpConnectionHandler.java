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

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventPublisher;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.reactivex.netty.events.Clock.*;
import static java.util.concurrent.TimeUnit.*;

public class HttpConnectionHandler<I, O> implements ConnectionHandler<HttpServerRequest<I>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(HttpConnectionHandler.class);

    private final RequestHandler<I, O> requestHandler;
    private final HttpServerEventPublisher eventPublisher;
    private final boolean sendHttp10ResponseFor10Request;

    public HttpConnectionHandler(RequestHandler<I, O> requestHandler, HttpServerEventPublisher eventPublisher,
                                 boolean sendHttp10ResponseFor10Request) {
        this.requestHandler = requestHandler;
        this.eventPublisher = eventPublisher;
        this.sendHttp10ResponseFor10Request = sendHttp10ResponseFor10Request;
    }

    @Override
    public Observable<Void> handle(final Connection<HttpServerRequest<I>, Object> c) {
        return c.getInput()
                .nest()
                .concatMap(new Func1<Observable<HttpServerRequest<I>>, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Observable<HttpServerRequest<I>> reqSource) {
                        return reqSource.take(1)
                                        .flatMap(new Func1<HttpServerRequest<I>, Observable<Void>>() {
                                            @Override
                                            public Observable<Void> call(HttpServerRequest<I> req) {
                                                final long startNanos = eventPublisher.publishingEnabled()
                                                                                ? Clock.newStartTimeNanos()
                                                                                : -1;

                                                if (eventPublisher.publishingEnabled()) {
                                                    eventPublisher.onNewRequestReceived();
                                                }

                                                final HttpServerResponse<O> response = newResponse(req, c);
                                                return handleRequest(req, startNanos, response, c);
                                            }
                                        });
                    }
                })
                .repeat()
                .ambWith(c.closeListener());
    }

    @SuppressWarnings("unchecked")
    private Observable<Void> handleRequest(HttpServerRequest<I> request, final long startTimeNanos,
                                           final HttpServerResponse<O> response,
                                           final Connection<HttpServerRequest<I>, Object> c) {
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
                               .concatWith(c.close())
                               .onErrorResumeNext(Observable.<Void>empty());// Ignore errors on cleanup
            }
        }).concatWith(request.dispose()/*Dispose request at the end of processing to discard content if not read*/
        ).concatWith(response.dispose()/*Dispose response at the end of processing to cleanup*/);

    }

    private HttpServerResponse<O> newResponse(HttpServerRequest<I> request,
                                              final Connection<HttpServerRequest<I>, Object> c) {

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
        HttpServerResponse<O> response = HttpServerResponseImpl.create(request, c, responseHeaders);
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
