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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;

/**
 * A factory to create {@link Connection}s for clients.
 *
 * @param <W> Type of object that is written to the client using this factory.
 * @param <R> Type of object that is read from the the client using this factory.
 *
 * @author Nitesh Kant
 */
public abstract class ClientConnectionFactory<W, R> {

    private ClientState<W, R> clientState;
    private volatile boolean isShutdown;

    protected ClientConnectionFactory(ClientState<W, R> clientState) {
        this.clientState = clientState;
    }

    /**
     * Returns an {@link Observable} that always returns a single {@link ObservableConnection}. This is a cold
     * observable and hence returns a potentially different connection on every subscription.
     *
     * @return {@link Observable} that always returns a single {@link ObservableConnection}.
     */
    public abstract Observable<? extends Connection<R, W>> connect();

    /**
     * Creates a copy of this instance by replacing the current {@link ClientState} with the passed {@code newState}
     *
     * @param newState New state to use in the copy.
     *
     * @return A copy of this instance.
     */
    public final <WW, RR> ClientConnectionFactory<WW, RR> copy(ClientState<WW, RR> newState) {
        ClientConnectionFactory<WW, RR>  clone = doCopy(newState);
        clone.clientState = newState;
        return clone;
    }

    /**
     * Shutdown this factory, to clear any state held internally.
     */
    public void shutdown() {
        isShutdown = true;
    }

    protected boolean isShutdown() {
        return isShutdown;
    }

    /**
     * A twin of {@link #copy(ClientState)} for implementations to provide the actual copy semantics.
     * {@link #copy(ClientState)} makes sure that the update in {@link ClientState} reflects  in this class.
     *
     * @param newState New state to use in the copy.
     *
     * @return Copy of this instance.
     */
    protected abstract <WW, RR> ClientConnectionFactory<WW, RR>  doCopy(ClientState<WW, RR>  newState);

    /**
     * Does the actual connection to the provided {@code remoteAddress}
     *
     * @return The future for the connection.
     */
    protected final ChannelFuture doConnect() {
        final long startTimeMillis = Clock.newStartTimeMillis();
        final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = clientState.getEventsSubject();
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);

        return clientState.getBootstrap().connect(clientState.getRemoteAddress())
                          .addListener(new ChannelFutureListener() {
                              @Override
                              public void operationComplete(ChannelFuture future) throws Exception {
                                  if (!future.isSuccess()) {
                                      clientState.getEventsSubject().onEvent(ClientMetricsEvent.CONNECT_FAILED,
                                                                             Clock.onEndMillis(startTimeMillis),
                                                                             future.cause());
                                  } else {
                                      clientState.getEventsSubject().onEvent(ClientMetricsEvent.CONNECT_SUCCESS,
                                                                             Clock.onEndMillis(startTimeMillis));
                                  }
                              }
                          });
    }
}
