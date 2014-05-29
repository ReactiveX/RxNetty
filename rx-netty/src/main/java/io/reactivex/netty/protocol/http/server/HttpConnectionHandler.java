/*
 * Copyright 2014 Netflix, Inc.
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

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

/**
* @author Nitesh Kant
*/
class HttpConnectionHandler<I, O> implements ConnectionHandler<HttpServerRequest<I>, HttpServerResponse<O>> {

    private ErrorResponseGenerator<O> responseGenerator = new DefaultErrorResponseGenerator<O>();

    private final RequestHandler<I, O> requestHandler;
    private final boolean send10ResponseFor10Request;

    public HttpConnectionHandler(RequestHandler<I, O> requestHandler) {
        this(requestHandler, false);
    }

    public HttpConnectionHandler(RequestHandler<I, O> requestHandler, boolean send10ResponseFor10Request) {
        this.requestHandler = requestHandler;
        this.send10ResponseFor10Request = send10ResponseFor10Request;
    }

    void setResponseGenerator(ErrorResponseGenerator<O> responseGenerator) {
        this.responseGenerator = responseGenerator;
    }

    @Override
    public Observable<Void> handle(final ObservableConnection<HttpServerRequest<I>, HttpServerResponse<O>> newConnection) {

        return newConnection.getInput().flatMap(new Func1<HttpServerRequest<I>, Observable<Void>>() {
            @Override
            public Observable<Void> call(HttpServerRequest<I> newRequest) {
                final HttpServerResponse<O> response = new HttpServerResponse<O>(
                        newConnection.getChannelHandlerContext(),
                        /*
                         * Server should send the highest version it is compatible with.
                         * http://tools.ietf.org/html/rfc2145#section-2.3
                         *
                         * unless overriden explicitly.
                         */
                        send10ResponseFor10Request ? newRequest.getHttpVersion() : HttpVersion.HTTP_1_1);
                if (newRequest.getHeaders().isKeepAlive()) {
                    // Add keep alive header as per:
                    // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                    response.getHeaders().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                } else {
                    response.getHeaders().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
                }
                Observable<Void> toReturn;

                try {
                    toReturn = requestHandler.handle(newRequest, response);
                    if (null == toReturn) {
                        toReturn = Observable.empty();
                    }
                } catch (Throwable throwable) {
                    toReturn = Observable.error(throwable);
                }

                return toReturn
                        .onErrorResumeNext(new Func1<Throwable, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(Throwable throwable) {
                                if (!response.isHeaderWritten()) {
                                    responseGenerator.updateResponse(response, throwable);
                                }
                                return Observable.empty();
                            }
                        })
                        .finallyDo(new Action0() {
                            @Override
                            public void call() {
                                response.close();
                            }
                        });
            }
        });
    }
}
