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
package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;

/**
 * A builder to build an instance of {@link RxClientImpl}
 *
 * @author Nitesh Kant
 */
public class ClientBuilder<I, O> extends AbstractClientBuilder<I,O, ClientBuilder<I, O>, RxClient<I, O>> {

    public ClientBuilder(String host, int port) {
        super(host, port, new TcpClientChannelAbstractFactory<I, O>());
    }

    public ClientBuilder(String host, int port, Bootstrap bootstrap) {
        super(bootstrap, host, port, new TcpClientChannelAbstractFactory<I, O>());
    }

    @Override
    protected RxClient<I, O> createClient() {
        return new RxClientImpl<I, O>(serverInfo, bootstrap, pipelineConfigurator, clientConfig, clientChannelFactory,
                                      connectionPool);
    }
}
