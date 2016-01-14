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

import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionRequest;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import rx.Subscription;

public class InterceptingTcpClientImpl<W, R> extends InterceptingTcpClient<W, R> {

    private final ConnectionProvider<W, R> cp;
    private final TcpClientEventPublisher eventPublisher;
    private final ConnectionRequest<W, R> connectionRequest;

    public InterceptingTcpClientImpl(ConnectionProvider<W, R> cp, TcpClientEventPublisher ep) {
        this.cp = cp;
        this.eventPublisher = ep;
        connectionRequest = new ConnectionRequestImpl<>(this.cp);

    }

    @Override
    public ConnectionRequest<W, R> createConnectionRequest() {
        return connectionRequest;
    }

    @Override
    public TcpClientInterceptorChain<W, R> intercept() {
        return new TcpClientInterceptorChainImpl<>(cp, eventPublisher);
    }

    @Override
    public Subscription subscribe(TcpClientEventListener listener) {
        return eventPublisher.subscribe(listener);
    }
}
