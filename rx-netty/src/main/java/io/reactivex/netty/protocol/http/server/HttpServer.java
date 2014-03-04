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
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.server.RxServer;

/**
 * @author Nitesh Kant
 */
public class HttpServer<I, O> extends RxServer<HttpRequest<I>, HttpResponse<O>> {

    private final HttpConnectionHandler<I, O> connectionHandler;

    public HttpServer(ServerBootstrap bootstrap, int port,
                      PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> pipelineConfigurator,
                      RequestHandler<I, O> requestHandler) {
        this(bootstrap, port, pipelineConfigurator, new HttpConnectionHandler<I, O>(requestHandler));
    }

    protected HttpServer(ServerBootstrap bootstrap, int port,
               PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> pipelineConfigurator,
               HttpConnectionHandler<I, O> connectionHandler) {
        super(bootstrap, port, addRequiredConfigurator(pipelineConfigurator), connectionHandler);
        this.connectionHandler = connectionHandler;
    }

    HttpServer<I, O> withErrorResponseGenerator(ErrorResponseGenerator<O> responseGenerator) {
        if (null == responseGenerator) {
            throw new IllegalArgumentException("Response generator can not be null.");
        }
        connectionHandler.setResponseGenerator(responseGenerator);
        return this;
    }

    private static <I, O> PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> addRequiredConfigurator(
            PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> pipelineConfigurator) {
        return new PipelineConfiguratorComposite<HttpRequest<I>, HttpResponse<O>>(pipelineConfigurator,
                                                                                  new ServerRequiredConfigurator<I, O>());
    }
}
