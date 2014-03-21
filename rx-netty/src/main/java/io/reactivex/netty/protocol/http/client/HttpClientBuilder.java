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
package io.reactivex.netty.protocol.http.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.client.AbstractClientBuilder;
import io.reactivex.netty.pipeline.PipelineConfigurators;

/**
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 *
 * @author Nitesh Kant
 */
public class HttpClientBuilder<I, O>
        extends AbstractClientBuilder<HttpClientRequest<I>, HttpClientResponse<O>, HttpClientBuilder<I, O>, HttpClient<I, O>> {

    public HttpClientBuilder(String host, int port) {
        super(host, port);
        clientConfig = HttpClient.HttpClientConfig.DEFAULT_CONFIG;
        pipelineConfigurator(PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    public HttpClientBuilder(Bootstrap bootstrap, String host, int port) {
        super(bootstrap, host, port);
        pipelineConfigurator(PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    @Override
    protected HttpClient<I, O> createClient() {
        return new HttpClientImpl<I, O>(serverInfo, bootstrap, pipelineConfigurator, clientConfig, channelPool);
    }
}
