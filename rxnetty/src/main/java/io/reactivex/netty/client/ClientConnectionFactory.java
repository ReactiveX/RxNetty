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

import io.netty.channel.Channel;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.ObservableConnectionFactory;

/**
 * @author Nitesh Kant
 */
public interface ClientConnectionFactory<I, O, C extends ObservableConnection<I, O>>
        extends ObservableConnectionFactory<I, O> {

    @Override
    C newConnection(Channel channel);
}
