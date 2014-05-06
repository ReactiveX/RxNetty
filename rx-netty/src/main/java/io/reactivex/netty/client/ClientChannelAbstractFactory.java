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
import io.reactivex.netty.channel.ObservableConnectionFactory;

/**
 * An abstract factory for creating {@link ClientChannelFactory}
 *
 * @author Nitesh Kant
 */
public interface ClientChannelAbstractFactory<I, O> {

    ClientChannelFactory<I, O> newClientChannelFactory(RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
                                                       ObservableConnectionFactory<I, O> connectionFactory);
}
