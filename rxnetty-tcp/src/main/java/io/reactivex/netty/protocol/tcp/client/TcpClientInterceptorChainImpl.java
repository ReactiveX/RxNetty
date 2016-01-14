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
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;

public class TcpClientInterceptorChainImpl<W, R> implements TcpClientInterceptorChain<W, R> {

    private ConnectionProvider<W, R> connectionProvider;
    private final TcpClientEventPublisher eventPublisher;

    public TcpClientInterceptorChainImpl(ConnectionProvider<W, R> cp, TcpClientEventPublisher ep) {
        connectionProvider = cp;
        this.eventPublisher = ep;
    }

    @Override
    public TcpClientInterceptorChain<W, R> next(Interceptor<W, R> i) {
        connectionProvider = i.intercept(connectionProvider);
        return this;
    }

    @Override
    public <RR> TcpClientInterceptorChain<W, RR> nextWithReadTransform(TransformingInterceptor<W, R, W, RR> i) {
        return new TcpClientInterceptorChainImpl<>(i.intercept(connectionProvider), eventPublisher);
    }

    @Override
    public <WW> TcpClientInterceptorChain<WW, R> nextWithWriteTransform(TransformingInterceptor<W, R, WW, R> i) {
        return new TcpClientInterceptorChainImpl<>(i.intercept(connectionProvider), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClientInterceptorChain<WW, RR> nextWithTransform(TransformingInterceptor<W, R, WW, RR> i) {
        return new TcpClientInterceptorChainImpl<>(i.intercept(connectionProvider), eventPublisher);
    }

    @Override
    public InterceptingTcpClient<W, R> finish() {
        return new InterceptingTcpClientImpl<>(connectionProvider, eventPublisher);
    }
}
