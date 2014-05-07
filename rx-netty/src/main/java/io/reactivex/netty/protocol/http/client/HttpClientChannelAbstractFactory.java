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
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.client.ClientChannelAbstractFactory;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.RxClient;

/**
 * @author Nitesh Kant
 */
public class HttpClientChannelAbstractFactory<I, O>
        implements ClientChannelAbstractFactory<HttpClientResponse<O>, HttpClientRequest<I>> {

    @Override
    public ClientChannelFactory<HttpClientResponse<O>, HttpClientRequest<I>> newClientChannelFactory(
            RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
            ObservableConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>> connectionFactory) {
        return new HttpClientChannelFactory<I, O>(clientBootstrap, connectionFactory, serverInfo);
    }
}
