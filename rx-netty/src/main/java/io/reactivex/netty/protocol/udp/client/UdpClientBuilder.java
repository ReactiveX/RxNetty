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
package io.reactivex.netty.protocol.udp.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.client.AbstractClientBuilder;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.RxClientImpl;

/**
 * A builder to build an instance of {@link RxClientImpl}
 *
 * @author Nitesh Kant
 */
public class UdpClientBuilder<I, O> extends AbstractClientBuilder<I,O, UdpClientBuilder<I, O>, RxClient<I, O>> {

    public UdpClientBuilder(String host, int port) {
        this(host, port, new Bootstrap());
    }

    public UdpClientBuilder(String host, int port, Bootstrap bootstrap) {
        super(bootstrap, host, port, new UdpClientChannelAbstractFactory<O, I>());
    }

    @Override
    protected RxClient<I, O> createClient() {
        return new UdpClient<I, O>(serverInfo, bootstrap, pipelineConfigurator, clientConfig, clientChannelFactory);
    }

    @Override
    protected boolean shouldCreateConnectionPool() {
        return false; // No connection pools are needed for UDP.
    }
}
