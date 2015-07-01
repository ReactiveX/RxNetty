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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.channel.PrimitiveConversionHandler;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionToChannelBridge.ClientConnectionSubscriberEvent;
import io.reactivex.netty.protocol.tcp.client.ConnectionObservable.OnSubcribeFunc;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.internal.EventPublisherFactory;
import io.reactivex.netty.protocol.tcp.internal.LoggingHandlerFactory;
import io.reactivex.netty.protocol.tcp.ssl.DefaultSslCodec;
import io.reactivex.netty.protocol.tcp.ssl.SslCodec;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static io.reactivex.netty.codec.HandlerNames.*;

/**
 * A collection of state that {@link TcpClient} holds. This supports the copy-on-write semantics of {@link TcpClient}
 *
 * @param <W> The type of objects written to the client owning this state.
 * @param <R> The type of objects read from the client owning this state.
 */
public class ClientState<W, R> extends ConnectionFactory<W, R> {

    private final ConnectionProvider<W, R> rawConnectionProvider;
    private volatile ConnectionProvider<W, R> realizedConnectionProvider; /*Realized once when started*/
    private final EventPublisherFactory eventPublisherFactory;
    private final DetachedChannelPipeline detachedPipeline;
    private final Map<ChannelOption<?>, Object> options;
    private final boolean isSecure;

    private ClientState(EventPublisherFactory eventPublisherFactory, DetachedChannelPipeline detachedPipeline,
                        ConnectionProvider<W, R> rawConnectionProvider) {
        this.rawConnectionProvider = rawConnectionProvider;
        options = new LinkedHashMap<>(); /// Same as netty bootstrap, order matters.
        this.eventPublisherFactory = eventPublisherFactory;
        isSecure = false;
        this.detachedPipeline = detachedPipeline;
    }

    private ClientState(ClientState<W, R> toCopy, ChannelOption<?> option, Object value) {
        options = new LinkedHashMap<>(toCopy.options); // Since, we are adding an option, copy it.
        options.put(option, value);
        rawConnectionProvider = toCopy.rawConnectionProvider;
        detachedPipeline = toCopy.detachedPipeline;
        isSecure = toCopy.isSecure;
        eventPublisherFactory = toCopy.eventPublisherFactory;
    }

    private ClientState(ClientState<W, R> toCopy, SslCodec sslCodec) {
        rawConnectionProvider = toCopy.rawConnectionProvider;
        options = toCopy.options; // Options are copied on change, so no need to make an eager copy.
        eventPublisherFactory = toCopy.eventPublisherFactory.copy();
        detachedPipeline = toCopy.detachedPipeline.copy(new TailHandlerFactory(true))
                                                  .configure(sslCodec);
        isSecure = true;
    }

    private ClientState(ClientState<?, ?> toCopy, DetachedChannelPipeline newPipeline) {
        final ClientState<W, R> toCopyCast = toCopy.cast();
        options = toCopy.options; // Options are copied on change, so no need to make an eager copy.
        rawConnectionProvider = toCopyCast.rawConnectionProvider;
        eventPublisherFactory = toCopyCast.eventPublisherFactory.copy();
        detachedPipeline = newPipeline;
        isSecure = toCopy.isSecure;
    }

    public <T> ClientState<W, R> channelOption(ChannelOption<T> option, T value) {
        return new ClientState<W, R>(this, option, value);
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addFirst(name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addFirst(group, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addLast(name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addLast(group, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addBefore(baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addBefore(group, baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addAfter(baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                               String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addAfter(group, baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.configure(pipelineConfigurator);
        return copy;
    }

    public ClientState<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return secure(new DefaultSslCodec(sslEngineFactory));
    }

    public ClientState<W, R> secure(SSLEngine sslEngine) {
        return secure(new DefaultSslCodec(sslEngine));
    }

    public ClientState<W, R> secure(SslCodec sslCodec) {
        ClientState<W, R> toReturn = new ClientState<W, R>(this, sslCodec);
        toReturn.realizedConnectionProvider = toReturn.rawConnectionProvider.realize(toReturn);
        return toReturn;
    }

    public ClientState<W, R> unsafeSecure() {
        return secure(new DefaultSslCodec(new Func1<ByteBufAllocator, SSLEngine>() {
            @Override
            public SSLEngine call(ByteBufAllocator allocator) {
                try {
                    return SslContextBuilder.forClient()
                                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                            .build()
                                            .newEngine(allocator);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        }));
    }

    public ClientState<W, R> enableWireLogging(final LogLevel wireLogginLevel) {
        return addChannelHandlerFirst(WireLogging.getName(), LoggingHandlerFactory.getFactory(wireLogginLevel));
    }

    public ConnectionProvider<W, R> getConnectionProvider() {
        return realizedConnectionProvider;
    }

    public static <WW, RR> ClientState<WW, RR> create(ConnectionProvider<WW, RR> connectionProvider,
                                                      EventPublisherFactory eventPublisherFactory) {
        final TailHandlerFactory tail = new TailHandlerFactory(false);
        DetachedChannelPipeline detachedPipeline = new DetachedChannelPipeline(tail)
                .addLast(PrimitiveConverter.getName(), new Func0<ChannelHandler>() {
                    @Override
                    public ChannelHandler call() {
                        return PrimitiveConversionHandler.INSTANCE;
                    }
                });

        return create(detachedPipeline, eventPublisherFactory, connectionProvider);
    }

    @Override
    public ConnectionObservable<R, W> newConnection(final SocketAddress hostAddress) {

        return ConnectionObservable.createNew(new OnSubcribeFunc<R, W>() {

            private final ListenersHolder<TcpClientEventListener> listeners = new ListenersHolder<>();

            @Override
            public void call(final Subscriber<? super Connection<R, W>> subscriber) {

                //TODO: Optimize this by not creating a new bootstrap everytime.
                final Bootstrap nettyBootstrap = newBootstrap(listeners);

                final ChannelFuture connectFuture = nettyBootstrap.connect(hostAddress);

                connectFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        connectFuture.channel()
                                     .pipeline()
                                     .fireUserEventTriggered(new ClientConnectionSubscriberEvent<R, W>(connectFuture,
                                                                                                       subscriber));
                    }
                });
            }

            @Override
            public Subscription subscribeForEvents(TcpClientEventListener eventListener) {
                return listeners.subscribe(eventListener);
            }
        });
    }

    /*Visible for testing*/ Bootstrap newBootstrap(ListenersHolder<TcpClientEventListener> listeners) {
        final Bootstrap nettyBootstrap = new Bootstrap().group(rawConnectionProvider.getEventLoopGroup())
                                                        .channel(rawConnectionProvider.getChannelClass())
                                                        .option(ChannelOption.AUTO_READ, false);// by default do not read content unless asked.

        for (Entry<ChannelOption<?>, Object> optionEntry : options.entrySet()) {
            // Type is just for safety for user of ClientState, internally in Bootstrap, types are thrown on the floor.
            @SuppressWarnings("unchecked")
            ChannelOption<Object> key = (ChannelOption<Object>) optionEntry.getKey();
            nettyBootstrap.option(key, optionEntry.getValue());
        }

        nettyBootstrap.handler(detachedPipeline.getChannelInitializer(new ChannelInitializer(listeners)));
        return nettyBootstrap;
    }

    EventPublisherFactory getEventPublisherFactory() {
        return eventPublisherFactory;
    }

    /*Visible for testing*/ static <WW, RR> ClientState<WW, RR> create(DetachedChannelPipeline detachedPipeline,
                                                                       EventPublisherFactory eventPublisherFactory,
                                                                       ConnectionProvider<WW, RR> connectionProvider) {
        ClientState<WW, RR> toReturn = new ClientState<>(eventPublisherFactory, detachedPipeline, connectionProvider);
        toReturn.realizedConnectionProvider = toReturn.rawConnectionProvider.realize(toReturn);
        return toReturn;
    }

    public DetachedChannelPipeline unsafeDetachedPipeline() {
        return detachedPipeline;
    }

    public Map<ChannelOption<?>, Object> unsafeChannelOptions() {
        return options;
    }

    private <WW, RR> ClientState<WW, RR> copy() {
        TailHandlerFactory newTail = new TailHandlerFactory(isSecure);
        ClientState<WW, RR> copy = new ClientState<WW, RR>(this, detachedPipeline.copy(newTail));
        copy.realizedConnectionProvider = copy.rawConnectionProvider.realize(copy);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private <WW, RR> ClientState<WW, RR> cast() {
        return (ClientState<WW, RR>) this;
    }

    private static class TailHandlerFactory implements Action1<ChannelPipeline> {

        private final boolean isSecure;

        public TailHandlerFactory(boolean isSecure) {
            this.isSecure = isSecure;
        }

        @Override
        public void call(ChannelPipeline pipeline) {
            ClientConnectionToChannelBridge.addToPipeline(pipeline, isSecure);
        }
    }

    private class ChannelInitializer implements Action1<Channel> {

        private final ListenersHolder<TcpClientEventListener> listeners;

        public ChannelInitializer(ListenersHolder<TcpClientEventListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void call(Channel channel) {
            EventSource<TcpClientEventListener> perChannelSource = eventPublisherFactory.call(channel);
            listeners.subscribeAllTo(perChannelSource);
        }
    }
}
