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
package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.HandlerNames;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.channel.PrimitiveConversionHandler;
import io.reactivex.netty.client.ClientConnectionToChannelBridge.ClientConnectionSubscriberEvent;
import io.reactivex.netty.client.ConnectionObservable.OnSubcribeFunc;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.client.internal.EventPublisherFactory;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.util.LoggingHandlerFactory;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;

import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * A collection of state that a client holds. This supports the copy-on-write semantics of clients.
 *
 * @param <W> The type of objects written to the client owning this state.
 * @param <R> The type of objects read from the client owning this state.
 */
public class ClientState<W, R> extends ConnectionFactory<W, R> {

    protected final ConnectionProvider<W, R> rawConnectionProvider;
    protected volatile ConnectionProvider<W, R> realizedConnectionProvider; /*Realized once when started*/
    protected final EventPublisherFactory<? extends ClientEventListener> eventPublisherFactory;
    protected final DetachedChannelPipeline detachedPipeline;
    protected final Map<ChannelOption<?>, Object> options;
    protected final boolean isSecure;

    protected ClientState(EventPublisherFactory<? extends ClientEventListener> eventPublisherFactory,
                          DetachedChannelPipeline detachedPipeline,
                          ConnectionProvider<W, R> rawConnectionProvider) {
        this.rawConnectionProvider = rawConnectionProvider;
        options = new LinkedHashMap<>(); /// Same as netty bootstrap, order matters.
        this.eventPublisherFactory = eventPublisherFactory;
        this.detachedPipeline = detachedPipeline;
        isSecure = false;
    }

    protected ClientState(ClientState<W, R> toCopy, ChannelOption<?> option, Object value) {
        options = new LinkedHashMap<>(toCopy.options); // Since, we are adding an option, copy it.
        options.put(option, value);
        rawConnectionProvider = toCopy.rawConnectionProvider;
        detachedPipeline = toCopy.detachedPipeline;
        eventPublisherFactory = toCopy.eventPublisherFactory;
        isSecure = toCopy.isSecure;
    }

    protected ClientState(ClientState<?, ?> toCopy, DetachedChannelPipeline newPipeline, boolean secure) {
        final ClientState<W, R> toCopyCast = toCopy.cast();
        options = toCopy.options;
        rawConnectionProvider = toCopyCast.rawConnectionProvider;
        eventPublisherFactory = toCopyCast.eventPublisherFactory.copy();
        detachedPipeline = newPipeline;
        isSecure = secure;
    }

    public <T> ClientState<W, R> channelOption(ChannelOption<T> option, T value) {
        return new ClientState<>(this, option, value);
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

    public ClientState<W, R> enableWireLogging(final LogLevel wireLoggingLevel) {
        return addChannelHandlerFirst(HandlerNames.WireLogging.getName(),
                                      LoggingHandlerFactory.getFactory(wireLoggingLevel));
    }

    public ConnectionProvider<W, R> getConnectionProvider() {
        return realizedConnectionProvider;
    }

    public static <WW, RR> ClientState<WW, RR> create(ConnectionProvider<WW, RR> connectionProvider,
                                                      EventPublisherFactory<ClientEventListener> epf) {
        return create(newChannelPipeline(new TailHandlerFactory(false)), epf, connectionProvider);
    }

    protected static DetachedChannelPipeline newChannelPipeline(TailHandlerFactory thf) {
        return new DetachedChannelPipeline(thf)
                .addLast(HandlerNames.PrimitiveConverter.getName(), new Func0<ChannelHandler>() {
                    @Override
                    public ChannelHandler call() {
                        return PrimitiveConversionHandler.INSTANCE;
                    }
                });
    }

    public static <WW, RR> ClientState<WW, RR> create(DetachedChannelPipeline detachedPipeline,
                                                      EventPublisherFactory<ClientEventListener> epf,
                                                      ConnectionProvider<WW, RR> connectionProvider) {
        ClientState<WW, RR> toReturn = new ClientState<>(epf, detachedPipeline, connectionProvider);
        toReturn.realizeState();
        return toReturn;
    }

    @Override
    public ConnectionObservable<R, W> newConnection(final SocketAddress hostAddress) {

        return ConnectionObservable.createNew(new OnSubcribeFunc<R, W>() {

            private final ListenersHolder<ClientEventListener> listeners = new ListenersHolder<>();

            @Override
            public void call(final Subscriber<? super Connection<R, W>> subscriber) {

                //TODO: Optimize this by not creating a new bootstrap every time.
                final Bootstrap nettyBootstrap = newBootstrap(listeners);

                final ChannelFuture connectFuture = nettyBootstrap.connect(hostAddress);

                connectFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        connectFuture.channel()
                                     .pipeline()
                                     .fireUserEventTriggered(new ClientConnectionSubscriberEvent<>(connectFuture,
                                                                                                   subscriber));
                    }
                });
            }

            @Override
            public Subscription subscribeForEvents(ClientEventListener eventListener) {
                return listeners.subscribe(eventListener);
            }
        });
    }

    /*Visible for testing*/ Bootstrap newBootstrap(ListenersHolder<ClientEventListener> listeners) {
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

    public EventPublisherFactory<? extends ClientEventListener> getEventPublisherFactory() {
        return eventPublisherFactory;
    }

    public DetachedChannelPipeline unsafeDetachedPipeline() {
        return detachedPipeline;
    }

    public Map<ChannelOption<?>, Object> unsafeChannelOptions() {
        return options;
    }

    protected <WW, RR> ClientState<WW, RR> copyStateInstance() {
        return new ClientState<>(this, detachedPipeline.copy(new TailHandlerFactory(isSecure)), isSecure);
    }

    protected void realizeState() {
        realizedConnectionProvider = rawConnectionProvider.realize(this);
    }

    private <WW, RR> ClientState<WW, RR> copy() {
        ClientState<WW, RR> copy = copyStateInstance();
        copy.realizeState();
        return copy;
    }

    @SuppressWarnings("unchecked")
    private <WW, RR> ClientState<WW, RR> cast() {
        return (ClientState<WW, RR>) this;
    }

    private class ChannelInitializer implements Action1<Channel> {

        private final ListenersHolder<ClientEventListener> listeners;

        public ChannelInitializer(ListenersHolder<ClientEventListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void call(Channel channel) {
            @SuppressWarnings("unchecked")
            EventSource<ClientEventListener> perChannelSource =
                    (EventSource<ClientEventListener>) eventPublisherFactory.call(channel);
            listeners.subscribeAllTo(perChannelSource);
        }
    }

    protected static class TailHandlerFactory implements Action1<ChannelPipeline> {

        private final boolean isSecure;

        public TailHandlerFactory(boolean isSecure) {
            this.isSecure = isSecure;
        }

        @Override
        public void call(ChannelPipeline pipeline) {
            ClientConnectionToChannelBridge.addToPipeline(pipeline, isSecure);
        }
    }
}
