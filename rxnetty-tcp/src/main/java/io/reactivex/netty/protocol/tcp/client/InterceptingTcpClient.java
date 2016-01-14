/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.protocol.tcp.client;

import io.reactivex.netty.client.ConnectionRequest;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;

public abstract class InterceptingTcpClient<W, R> implements EventSource<TcpClientEventListener> {

    /**
     * Creates a new {@link ConnectionRequest} which should be subscribed to actually connect to the target server.
     *
     * @return A new {@link ConnectionRequest} which either can be subscribed directly or altered in various ways
     * before subscription.
     */
    public abstract ConnectionRequest<W, R> createConnectionRequest();

    public abstract TcpClientInterceptorChain<W, R> intercept();
}
