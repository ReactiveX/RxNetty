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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.tcp.client.PooledConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.SafeSubscriber;
import rx.subscriptions.Subscriptions;

import java.net.SocketAddress;

/**
 * An implementation of {@link AbstractConnectionToChannelBridge} for clients.
 *
 * <h2>Reuse</h2>
 *
 * A channel can be reused for multiple operations, provided the reuses is signalled by {@link ConnectionResueEvent}.
 * Failure to do so, will result in errors on the {@link Subscriber} trying to reuse the channel.
 * A typical reuse should have the following events:
 *
 <PRE>
    ConnectionSubscriberEvent => ConnectionInputSubscriberEvent => ConnectionResueEvent =>
    ConnectionInputSubscriberEvent => ConnectionResueEvent => ConnectionInputSubscriberEvent
 </PRE>
 *
 * @param <R> Type read from the connection held by this handler.
 * @param <W> Type written to the connection held by this handler.
 */
public class ClientConnectionToChannelBridge<R, W> extends AbstractConnectionToChannelBridge<R, W> {

    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionToChannelBridge.class);

    private final boolean isSecure;

    public ClientConnectionToChannelBridge(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject, boolean isSecure) {
        super(eventsSubject, ClientChannelMetricEventProvider.INSTANCE);
        this.isSecure = isSecure;
    }

    public ClientConnectionToChannelBridge(Subscriber<? super Connection<R, W>> connSub,
                                           MetricEventsSubject<?> eventsSubject, boolean isSecure) {
        super(connSub, eventsSubject, ClientChannelMetricEventProvider.INSTANCE);
        this.isSecure = isSecure;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!isSecure) {/*When secure, the event is triggered post SSL handshake via the SslCodec*/
            userEventTriggered(ctx, EmitConnectionEvent.INSTANCE);
        }
        super.channelActive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        super.userEventTriggered(ctx, evt); // Super handles ConnectionInputSubscriberResetEvent to reset the subscriber.

        if (evt instanceof ClientConnectionSubscriberEvent) {
            @SuppressWarnings("unchecked")
            final ClientConnectionSubscriberEvent<R, W> event = (ClientConnectionSubscriberEvent<R, W>) evt;

            if (event.getSubscriber() == getNewConnectionSub()) { // If this subscriber wasn't a duplicate
                connectSubscriberToFuture(event.getSubscriber(), event.getConnectFuture());
            }
        } else if (evt instanceof ConnectionResueEvent) {
            @SuppressWarnings("unchecked")
            ConnectionResueEvent<R, W> event = (ConnectionResueEvent<R, W>) evt;

            newConnectionReuseEvent(ctx.channel(), event);
        }
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        final long startTimeMillis = Clock.newStartTimeMillis();
        promise.addListener(new ChannelFutureListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    eventsSubject.onEvent(ClientMetricsEvent.CONNECT_FAILED, Clock.onEndMillis(startTimeMillis),
                                          future.cause());
                } else {
                    eventsSubject.onEvent(ClientMetricsEvent.CONNECT_SUCCESS, Clock.onEndMillis(startTimeMillis));
                }
            }
        });
        super.connect(ctx, remoteAddress, localAddress, promise);
    }

    @Override
    protected void onNewReadSubscriber(final Channel channel, Subscriber<? super R> subscriber) {
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                // Unsubscribe from the input closes the connection as there can only be one subscriber to the
                // input and, if nothing is read, it means, nobody is using the connection.
                // For fire-and-forget usecases, one should explicitly ignore content on the connection which
                // adds a discard all subscriber that never unsubscribes. For this case, then, the close becomes
                // explicit.
                channel.close();
            }
        }));
    }

    private void connectSubscriberToFuture(final Subscriber<? super Connection<R, W>> subscriber,
                                           final ChannelFuture channelFuture) {
        // Set the subscription action to cancel the future on unsubscribe.
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (null != channelFuture && !channelFuture.isDone()) {
                    channelFuture.cancel(false);
                }
            }
        }));
        // Send an error to subscriber if connect fails.
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    if (null != subscriber) {
                        subscriber.onError(future.cause());
                    } else {
                        logger.error("Connection failed but no connection subscriber available.", future.cause());
                    }
                }
            }
        });
    }

    private void newConnectionReuseEvent(Channel channel, final ConnectionResueEvent<R, W> event) {
        Subscriber<? super PooledConnection<R, W>> subscriber = event.getSubscriber();
        if (isValidToEmit(subscriber)) {
            subscriber.onNext(event.getPooledConnection());
            checkEagerSubscriptionIfConfigured(event.getPooledConnection(), channel);
        } else {
            event.getPooledConnection().close(false); // If pooled connection not sent to the subscriber, release to the pool.
        }
    }


    /**
     * An event to communicate the subscriber of a new connection created by {@link AbstractConnectionToChannelBridge}.
     *
     * <h2>Connection reuse</h2>
     *
     * For cases, where the {@link Connection} is pooled, reuse should be indicated explicitly via
     * {@link ConnectionInputSubscriberResetEvent}. There can be multiple {@link ConnectionInputSubscriberResetEvent}s
     * sent to the same channel and hence the same instance of {@link AbstractConnectionToChannelBridge}.
     *
     * @param <I> Type read from the connection held by the event.
     * @param <O> Type written to the connection held by the event.
     */
    public static class ClientConnectionSubscriberEvent<I, O> extends ConnectionSubscriberEvent<I, O> {

        private final ChannelFuture connectFuture;

        public ClientConnectionSubscriberEvent(ChannelFuture connectFuture,
                                               Subscriber<? super Connection<I, O>> subscriber) {
            super(subscriber);
            this.connectFuture = connectFuture;
        }

        public ChannelFuture getConnectFuture() {
            return connectFuture;
        }
    }

    /**
     * An event to indicate channel/{@link ObservableConnection} reuse. This event should be used for clients that pool
     * connections. For every reuse of a connection (connection creation still uses {@link ConnectionSubscriberEvent})
     * the corresponding subscriber must be sent via this event.
     *
     * Every instance of this event resets the older subscriber attached to the connection and connection input. This
     * means sending an {@link Subscriber#onCompleted()} to both of those subscribers. It is assumed that the actual
     * {@link Subscriber} is similar to {@link SafeSubscriber} which can handle duplicate terminal events.
     *
     * @param <I> Type read from the connection held by the event.
     * @param <O> Type written to the connection held by the event.
     */
    public static final class ConnectionResueEvent<I, O> implements ConnectionInputSubscriberResetEvent {

        private final Subscriber<? super PooledConnection<I, O>> subscriber;
        private final PooledConnection<I, O> pooledConnection;

        public ConnectionResueEvent(Subscriber<? super PooledConnection<I, O>> subscriber,
                                    PooledConnection<I, O> pooledConnection) {
            this.subscriber = subscriber;
            this.pooledConnection = pooledConnection;
        }

        public Subscriber<? super PooledConnection<I, O>> getSubscriber() {
            return subscriber;
        }

        public PooledConnection<I, O> getPooledConnection() {
            return pooledConnection;
        }
    }

    /**
     * An event to indicate release of a {@link PooledConnection}.
     */
    public static final class PooledConnectionReleaseEvent {

        public static final PooledConnectionReleaseEvent INSTANCE = new PooledConnectionReleaseEvent();

        private PooledConnectionReleaseEvent() {
        }
    }
}
