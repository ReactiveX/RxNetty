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

import io.netty.bootstrap.ServerBootstrap;
import io.reactivex.netty.ConnectionHandler;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.server.RxServer;
import rx.Observable;
import rx.util.functions.Func1;

/**
 * @author Nitesh Kant
 */
public class HttpServer<I, O> extends RxServer<HttpRequest<I>, HttpResponse<O>> {

    public HttpServer(ServerBootstrap bootstrap, int port,
                      PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> pipelineConfigurator,
                      RequestHandler<I, O> requestHandler) {
        super(bootstrap, port, addRequiredConfigurator(pipelineConfigurator), new HttpConnectionHandler<I, O>(requestHandler));
    }

    HttpServer(ServerBootstrap bootstrap, int port,
               PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> pipelineConfigurator,
               HttpConnectionHandler<I, O> connectionHandler) {
        super(bootstrap, port, addRequiredConfigurator(pipelineConfigurator), connectionHandler);
    }

    private static <I, O> PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> addRequiredConfigurator(
            PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> pipelineConfigurator) {
        return new PipelineConfiguratorComposite<HttpRequest<I>, HttpResponse<O>>(pipelineConfigurator,
                                                                                  new ServerRequiredConfigurator<I, O>());
    }

    static class HttpConnectionHandler<I, O> implements ConnectionHandler<HttpRequest<I>, HttpResponse<O>> {

        private final RequestHandler<I, O> requestHandler;

        public HttpConnectionHandler(RequestHandler<I, O> requestHandler) {
            this.requestHandler = requestHandler;
        }

        @Override
        public Observable<Void> handle(final ObservableConnection<HttpRequest<I>, HttpResponse<O>> newConnection) {

            return newConnection.getInput().flatMap(new Func1<HttpRequest<I>, Observable<Void>>() {
                @Override
                public Observable<Void> call(HttpRequest<I> newRequest) {
                    final HttpResponse<O> response = new HttpResponse<O>(newConnection.getChannelHandlerContext(),
                                                                   newRequest.getHttpVersion());
                    return requestHandler.handle(newRequest, response).flatMap(new Func1<Void, Observable<Void>>() {
                        @Override
                        public Observable<Void> call(Void aVoid) {
                            return response.close();
                        }
                    });
                }
            });
        }
    }
}
