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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public class ClientEmbeddedConnectionFactory<W, R> extends ClientConnectionFactory<W, R> {

    private final ClientState<W, R> clientState;
    private final Func0<EmbeddedChannel> channelFactory;

    private ClientEmbeddedConnectionFactory(ClientState<W, R> clientState, Func0<EmbeddedChannel> channelFactory) {
        super(clientState);
        this.clientState = clientState;
        this.channelFactory = channelFactory;
    }

    @Override
    public Observable<? extends Connection<R, W>> connect() {
        EmbeddedChannel newChannel = channelFactory.call();
        clientState.getDetachedPipeline().copyTo(new EmbeddedChannelPipelineDelegate(newChannel));
        final ConnectionImpl<R, W> conn = ConnectionImpl.create(newChannel, clientState.getEventsSubject(),
                                                                ClientChannelMetricEventProvider.INSTANCE);
        return Observable.just(conn);
    }

    @Override
    protected <WW, RR> ClientConnectionFactory<WW, RR> doCopy(ClientState<WW, RR> newState) {
        return new ClientEmbeddedConnectionFactory<>(newState, channelFactory);
    }

    public static <W, R> Func1<ClientState<W, R>, ClientConnectionFactory<W, R>> newFactoryFunc(final Func0<EmbeddedChannel> channel) {
        return new Func1<ClientState<W, R>, ClientConnectionFactory<W, R>>() {
            @Override
            public ClientConnectionFactory<W, R> call(ClientState<W, R> clientState) {
                return new ClientEmbeddedConnectionFactory<>(clientState, channel);
            }
        };
    }

    private static class EmbeddedChannelPipelineDelegate implements ChannelPipeline {

        private final ChannelPipeline pipeline;
        private final String lastHandlerName;
        private final AtomicLong uniqueNameCounter = new AtomicLong();

        public EmbeddedChannelPipelineDelegate(EmbeddedChannel newChannel) {
            pipeline = newChannel.pipeline();
            final ChannelHandler lastHandler = pipeline.last();
            if (null != lastHandler) {
                List<String> names = pipeline.names();
                for (String name : names) {
                    if (pipeline.get(name) == lastHandler) {
                        lastHandlerName = name;
                        return;
                    }
                }
                lastHandlerName = null;
            } else {
                lastHandlerName = null;
            }
        }

        @Override
        public ChannelPipeline addFirst(String name, ChannelHandler handler) {
            pipeline.addFirst(name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addFirst(EventExecutorGroup group,
                                        String name, ChannelHandler handler) {
            pipeline.addFirst(group, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addFirst(ChannelHandlerInvoker invoker,
                                        String name, ChannelHandler handler) {
            pipeline.addFirst(invoker, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addLast(String name, ChannelHandler handler) {
            if (null != lastHandlerName) {
                pipeline.addBefore(lastHandlerName, name, handler);
            } else {
                pipeline.addLast(name, handler);
            }
            return this;
        }

        @Override
        public ChannelPipeline addLast(EventExecutorGroup group,
                                       String name, ChannelHandler handler) {
            if (null != lastHandlerName) {
                pipeline.addBefore(group, lastHandlerName, name, handler);
            } else {
                pipeline.addLast(group, name, handler);
            }
            return this;
        }

        @Override
        public ChannelPipeline addLast(ChannelHandlerInvoker invoker,
                                       String name, ChannelHandler handler) {
            if (null != lastHandlerName) {
                pipeline.addBefore(invoker, lastHandlerName, name, handler);
            } else {
                pipeline.addLast(invoker, name, handler);
            }
            return this;
        }

        @Override
        public ChannelPipeline addBefore(String baseName, String name,
                                         ChannelHandler handler) {
            pipeline.addBefore(baseName, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addBefore(EventExecutorGroup group,
                                         String baseName, String name,
                                         ChannelHandler handler) {
            pipeline.addBefore(group, baseName, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addBefore(ChannelHandlerInvoker invoker,
                                         String baseName, String name,
                                         ChannelHandler handler) {
            pipeline.addBefore(invoker, baseName, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addAfter(String baseName, String name,
                                        ChannelHandler handler) {
            pipeline.addAfter(baseName, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addAfter(EventExecutorGroup group,
                                        String baseName, String name,
                                        ChannelHandler handler) {
            pipeline.addAfter(group, baseName, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addAfter(ChannelHandlerInvoker invoker,
                                        String baseName, String name,
                                        ChannelHandler handler) {
            pipeline.addAfter(invoker, baseName, name, handler);
            return this;
        }

        @Override
        public ChannelPipeline addFirst(ChannelHandler... handlers) {
            pipeline.addFirst(handlers);
            return this;
        }

        @Override
        public ChannelPipeline addFirst(EventExecutorGroup group,
                                        ChannelHandler... handlers) {
            pipeline.addFirst(group, handlers);
            return this;
        }

        @Override
        public ChannelPipeline addFirst(ChannelHandlerInvoker invoker,
                                        ChannelHandler... handlers) {
            pipeline.addFirst(invoker, handlers);
            return this;
        }

        @Override
        public ChannelPipeline addLast(ChannelHandler... handlers) {
            if (null != lastHandlerName) {
                for (ChannelHandler handler : handlers) {
                    pipeline.addBefore(lastHandlerName, generateUniqueName(), handler);
                }
            } else {
                pipeline.addLast(handlers);
            }
            return this;
        }

        @Override
        public ChannelPipeline addLast(EventExecutorGroup group,
                                       ChannelHandler... handlers) {
            if (null != lastHandlerName) {
                for (ChannelHandler handler : handlers) {
                    pipeline.addBefore(group, lastHandlerName, generateUniqueName(), handler);
                }
            } else {
                pipeline.addLast(group, handlers);
            }
            return this;
        }

        @Override
        public ChannelPipeline addLast(ChannelHandlerInvoker invoker,
                                       ChannelHandler... handlers) {
            if (null != lastHandlerName) {
                for (ChannelHandler handler : handlers) {
                    pipeline.addBefore(invoker, lastHandlerName, generateUniqueName(), handler);
                }
            } else {
                pipeline.addLast(invoker, handlers);
            }
            return this;
        }

        @Override
        public ChannelPipeline remove(ChannelHandler handler) {
            return pipeline.remove(handler);
        }

        @Override
        public ChannelHandler remove(String name) {
            return pipeline.remove(name);
        }

        @Override
        public <T extends ChannelHandler> T remove(Class<T> handlerType) {
            return pipeline.remove(handlerType);
        }

        @Override
        public ChannelHandler removeFirst() {
            return pipeline.removeFirst();
        }

        @Override
        public ChannelHandler removeLast() {
            return pipeline.removeLast();
        }

        @Override
        public ChannelPipeline replace(ChannelHandler oldHandler,
                                       String newName, ChannelHandler newHandler) {
            return pipeline.replace(oldHandler, newName, newHandler);
        }

        @Override
        public ChannelHandler replace(String oldName, String newName,
                                      ChannelHandler newHandler) {
            return pipeline.replace(oldName, newName, newHandler);
        }

        @Override
        public <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName,
                                                    ChannelHandler newHandler) {
            return pipeline.replace(oldHandlerType, newName, newHandler);
        }

        private String generateUniqueName() {
            return "ClientEmbeddedConnectionFactoryGeneratedName-" + uniqueNameCounter.incrementAndGet();
        }

        @Override
        public ChannelHandler first() {
            return pipeline.first();
        }

        @Override
        public ChannelHandlerContext firstContext() {
            return pipeline.firstContext();
        }

        @Override
        public ChannelHandler last() {
            return pipeline.last();
        }

        @Override
        public ChannelHandlerContext lastContext() {
            return pipeline.lastContext();
        }

        @Override
        public ChannelHandler get(String name) {
            return pipeline.get(name);
        }

        @Override
        public <T extends ChannelHandler> T get(Class<T> handlerType) {
            return pipeline.get(handlerType);
        }

        @Override
        public ChannelHandlerContext context(ChannelHandler handler) {
            return pipeline.context(handler);
        }

        @Override
        public ChannelHandlerContext context(String name) {
            return pipeline.context(name);
        }

        @Override
        public ChannelHandlerContext context(
                Class<? extends ChannelHandler> handlerType) {
            return pipeline.context(handlerType);
        }

        @Override
        public Channel channel() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public List<String> names() {
            return pipeline.names();
        }

        @Override
        public Map<String, ChannelHandler> toMap() {
            return pipeline.toMap();
        }

        @Override
        public ChannelPipeline fireChannelRegistered() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireChannelUnregistered() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireChannelActive() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireChannelInactive() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireExceptionCaught(Throwable cause) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireUserEventTriggered(Object event) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireChannelRead(Object msg) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireChannelReadComplete() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline fireChannelWritabilityChanged() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress,
                                     SocketAddress localAddress) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture disconnect() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture close() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture deregister() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress,
                                  ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress,
                                     ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress,
                                     SocketAddress localAddress,
                                     ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture disconnect(ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture close(ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture deregister(ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline read() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture write(Object msg) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture write(Object msg, ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelPipeline flush() {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            throw new UnsupportedOperationException("Only pipeline modification operations are allowed");
        }

        @Override
        public Iterator<Entry<String, ChannelHandler>> iterator() {
            return pipeline.iterator();
        }
    }
}
