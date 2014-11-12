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
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.server.ErrorHandler;
import io.reactivex.netty.server.RxServer;

import java.util.List;

import static io.reactivex.netty.protocol.http.server.HttpServerRequest.DEFAULT_CONTENT_SUBSCRIPTION_TIMEOUT_MS;

/**
 * @author Nitesh Kant
 */
public class HttpServer<I, O> extends RxServer<HttpServerRequest<I>, HttpServerResponse<O>> {

    private final HttpConnectionHandler<I, O> connectionHandler;

    public HttpServer(ServerBootstrap bootstrap, int port,
                      PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> pipelineConfigurator,
                      RequestHandler<I, O> requestHandler) {
        this(bootstrap, port, pipelineConfigurator, requestHandler, null);
    }

    public HttpServer(ServerBootstrap bootstrap, int port,
                      PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> pipelineConfigurator,
                      RequestHandler<I, O> requestHandler, EventExecutorGroup requestProcessingExecutor) {
        this(bootstrap, port, pipelineConfigurator, new HttpConnectionHandler<I, O>(requestHandler),
             requestProcessingExecutor, DEFAULT_CONTENT_SUBSCRIPTION_TIMEOUT_MS);
    }

    protected HttpServer(ServerBootstrap bootstrap, int port,
               PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> pipelineConfigurator,
               HttpConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, pipelineConfigurator, connectionHandler, null, DEFAULT_CONTENT_SUBSCRIPTION_TIMEOUT_MS);
    }

    protected HttpServer(ServerBootstrap bootstrap, int port,
               PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> pipelineConfigurator,
               HttpConnectionHandler<I, O> connectionHandler, EventExecutorGroup requestProcessingExecutor,
               long requestContentSubscriptionTimeoutMs) {
        super(bootstrap, port, addRequiredConfigurator(pipelineConfigurator, requestProcessingExecutor,
                                                       requestContentSubscriptionTimeoutMs),
              connectionHandler, requestProcessingExecutor);
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<PipelineConfigurator> constituentConfigurators =
                ((PipelineConfiguratorComposite) this.pipelineConfigurator).getConstituentConfigurators();
        boolean updatedSubject = false;
        for (@SuppressWarnings("rawtypes") PipelineConfigurator configurator : constituentConfigurators) {
            if (configurator instanceof ServerRequiredConfigurator) {
                updatedSubject = true;
                @SuppressWarnings("unchecked")
                ServerRequiredConfigurator<I, O> requiredConfigurator = (ServerRequiredConfigurator<I, O>) configurator;
                requiredConfigurator.useMetricEventsSubject(eventsSubject);
            }
        }
        if (!updatedSubject) {
            throw new IllegalStateException("No server required configurator added.");
        }
        connectionHandler.useMetricEventsSubject(eventsSubject);
        this.connectionHandler = connectionHandler;
    }

    public HttpServer<I, O> withErrorResponseGenerator(ErrorResponseGenerator<O> responseGenerator) {
        if (null == responseGenerator) {
            throw new IllegalArgumentException("Response generator can not be null.");
        }
        connectionHandler.setResponseGenerator(responseGenerator);
        return this;
    }

    @Override
    public HttpServer<I, O> start() {
        super.start();
        return this;
    }

    @Override
    public HttpServer<I, O> withErrorHandler(ErrorHandler errorHandler) {
        super.withErrorHandler(errorHandler);
        return this;
    }

    private static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> addRequiredConfigurator(
            PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> pipelineConfigurator,
            EventExecutorGroup requestProcessingExecutor, long requestContentSubscriptionTimeoutMs) {
        return new PipelineConfiguratorComposite<HttpServerRequest<I>, HttpServerResponse<O>>(pipelineConfigurator,
                                                                                  new ServerRequiredConfigurator<I, O>(requestProcessingExecutor,
                                                                                                                       requestContentSubscriptionTimeoutMs));
    }
}
