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
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.client.ClientChannelAbstractFactory;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.RxClient;

import java.net.InetSocketAddress;

/**
 * @author Nitesh Kant
 */
public class UdpClientChannelAbstractFactory<I, O> implements ClientChannelAbstractFactory<I, O> {

    @Override
    public ClientChannelFactory<I, O> newClientChannelFactory(RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
                                                              ObservableConnectionFactory<I, O> connectionFactory) {
        final InetSocketAddress receiverAddress = new InetSocketAddress(serverInfo.getHost(), serverInfo.getPort());
        return new ClientChannelFactoryImpl<I, O>(clientBootstrap,
                                                  new UdpClientConnectionFactory<I, O>(receiverAddress), serverInfo);
    }
}
