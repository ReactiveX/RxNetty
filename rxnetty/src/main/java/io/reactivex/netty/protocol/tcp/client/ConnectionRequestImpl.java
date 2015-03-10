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
 */
package io.reactivex.netty.protocol.tcp.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.codec.SSLCodec;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;

import java.util.concurrent.TimeUnit;

final class ConnectionRequestImpl<W, R> extends ConnectionRequest<W, R> {

    private final ClientState<W, R> clientState;

    ConnectionRequestImpl(final ClientState<W, R> clientState) {
        super(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                clientState.getConnectionFactory()
                           .connect()
                           .subscribe(subscriber);
            }
        });
        this.clientState = clientState;
    }

    @Override
    public ConnectionRequest<W, R> readTimeOut(int timeOut, TimeUnit timeUnit) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public ConnectionRequest<W, R> enableWireLogging(LogLevel wireLogginLevel) {
        return copy(clientState.enableWireLogging(wireLogginLevel));
    }

    @Override
    public ConnectionRequest<W, R> sslEngineFactory(SSLEngineFactory sslEngineFactory) {
        return copy(clientState.<W, R>pipelineConfigurator(new SSLCodec(sslEngineFactory)));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerFirst(name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerFirst(group, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerLast(name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                    Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerLast(group, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                      Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerBefore(baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                      String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerBefore(group, baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                                     Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerAfter(baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                     String name, Func0<ChannelHandler> handlerFactory) {
        return copy(clientState.<WW, RR>addChannelHandlerAfter(group, baseName, name, handlerFactory));
    }

    @Override
    public <WW, RR> ConnectionRequest<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return copy(clientState.<WW, RR>pipelineConfigurator(pipelineConfigurator));
    }

    @Override
    public ConnectionRequestUpdater<W, R> newUpdater() {
        // TODO: Auto-generated method stub
        return null;
    }

    private static <WW, RR> ConnectionRequestImpl<WW, RR> copy(ClientState<WW, RR> state) {
        return new ConnectionRequestImpl<>(state);
    }
}
