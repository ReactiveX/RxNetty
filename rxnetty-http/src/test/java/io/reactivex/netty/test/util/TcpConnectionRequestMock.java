/*
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.netty.test.util;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.tcp.client.ConnectionRequest;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.util.concurrent.TimeUnit;

public class TcpConnectionRequestMock<W, R> extends ConnectionRequest<W, R> {

    public TcpConnectionRequestMock(final Observable<Connection<R, W>> connections) {
        super(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                connections.unsafeSubscribe(subscriber);
            }
        });
    }

    @Override
    public ConnectionRequest<W, R> readTimeOut(int timeOut, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionRequest<W, R> enableWireLogging(LogLevel wireLogginLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                    Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                      Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                      String name,
                                                                      Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                     String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionRequest<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionRequest<W, R> secure(SSLEngine sslEngine) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionRequest<W, R> secure(SslCodec sslCodec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionRequest<W, R> unsafeSecure() {
        throw new UnsupportedOperationException();
    }
}
