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
package io.reactivex.netty.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import io.reactivex.netty.channel.AbstractConnectionToChannelBridge;
import io.reactivex.netty.channel.ChannelSubscriberEvent;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionInputSubscriberResetEvent;
import io.reactivex.netty.channel.EmitConnectionEvent;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.client.pool.PooledConnection;
import io.reactivex.netty.events.EventAttributeKeys;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.internal.ExecuteInEventloopAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.observers.SafeSubscriber;
import rx.subscriptions.Subscriptions;

/**
 * An implementation of {@link AbstractConnectionToChannelBridge} for clients.
 *
 * <h2>Reuse</h2>
 *
 * A channel can be reused for multiple operations, provided the reuses is signalled by {@link ConnectionReuseEvent}.
 * Failure to do so, will result in errors on the {@link Subscriber} trying to reuse the channel.
 * A typical reuse should have the following events:
 *
 <PRE>
    ChannelSubscriberEvent => ConnectionInputSubscriberEvent => ConnectionReuseEvent =>
    ConnectionInputSubscriberEvent => ConnectionReuseEvent => ConnectionInputSubscriberEvent
 </PRE>
 *
 * @param <R> Type read from the connection held by this handler.
 * @param <W> Type written to the connection held by this handler.
 */
public class ClientConnectionToChannelBridge<R, W> extends AbstractConnectionToChannelBridge<R, W> {

    public static final AttributeKey<Boolean> DISCARD_CONNECTION = AttributeKey.valueOf("rxnetty_discard_connection");

    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionToChannelBridge.class);
    private static final String HANDLER_NAME = "client-conn-channel-bridge";

    private EventPublisher eventPublisher;
    private ClientEventListener eventListener;
    private final boolean isSecure;
    private Channel channel;

    private ClientConnectionToChannelBridge(boolean isSecure) {
        super(HANDLER_NAME, EventAttributeKeys.CONNECTION_EVENT_LISTENER, EventAttributeKeys.EVENT_PUBLISHER);
        this.isSecure = isSecure;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        eventPublisher = channel.attr(EventAttributeKeys.EVENT_PUBLISHER).get();
        eventListener = ctx.channel().attr(EventAttributeKeys.CLIENT_EVENT_LISTENER).get();

        if (null == eventPublisher) {
            logger.error("No Event publisher bound to the channel, closing channel.");
            ctx.channel().close();
            return;
        }

        if (eventPublisher.publishingEnabled() && null == eventListener) {
            logger.error("No Event listener bound to the channel and event publishing is enabled., closing channel.");
            ctx.channel().close();
            return;
        }

        super.handlerAdded(ctx);
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

        if (evt instanceof ConnectionReuseEvent) {
            @SuppressWarnings("unchecked")
            ConnectionReuseEvent<R, W> event = (ConnectionReuseEvent<R, W>) evt;

            newConnectionReuseEvent(ctx.channel(), event);
        }
    }

    @Override
    protected void onNewReadSubscriber(Subscriber<? super R> subscriber) {
        // Unsubscribe from the input closes the connection as there can only be one subscriber to the
        // input and, if nothing is read, it means, nobody is using the connection.
        // For fire-and-forget usecases, one should explicitly ignore content on the connection which
        // adds a discard all subscriber that never unsubscribes. For this case, then, the close becomes
        // explicit.
        subscriber.add(Subscriptions.create(new ExecuteInEventloopAction(channel) {
            @Override
            public void run() {
                if (!connectionInputSubscriberExists(channel)) {
                    Connection<?, ?> connection = channel.attr(Connection.CONNECTION_ATTRIBUTE_KEY).get();
                    if (null != connection) {
                        connection.closeNow();
                    }
                }
            }
        }));
    }

    private void newConnectionReuseEvent(Channel channel, final ConnectionReuseEvent<R, W> event) {
        Subscriber<? super PooledConnection<R, W>> subscriber = event.getSubscriber();
        if (isValidToEmit(subscriber)) {
            subscriber.onNext(event.getPooledConnection());
            checkEagerSubscriptionIfConfigured(channel);
        } else {
            // If pooled connection not sent to the subscriber, release to the pool.
            event.getPooledConnection().close(false).subscribe(Actions.empty(), new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    logger.error("Error closing connection.", throwable);
                }
            });
        }
    }

    public static <R, W> ClientConnectionToChannelBridge<R, W> addToPipeline(ChannelPipeline pipeline,
                                                                             boolean isSecure) {
        ClientConnectionToChannelBridge<R, W> toAdd = new ClientConnectionToChannelBridge<>(isSecure);
        pipeline.addLast(HANDLER_NAME, toAdd);
        return toAdd;
    }

    /**
     * An event to indicate channel/{@link Connection} reuse. This event should be used for clients that pool
     * connections. For every reuse of a connection (connection creation still uses {@link ChannelSubscriberEvent})
     * the corresponding subscriber must be sent via this event.
     *
     * Every instance of this event resets the older subscriber attached to the connection and connection input. This
     * means sending an {@link Subscriber#onCompleted()} to both of those subscribers. It is assumed that the actual
     * {@link Subscriber} is similar to {@link SafeSubscriber} which can handle duplicate terminal events.
     *
     * @param <I> Type read from the connection held by the event.
     * @param <O> Type written to the connection held by the event.
     */
    public static final class ConnectionReuseEvent<I, O> implements ConnectionInputSubscriberResetEvent {

        private final Subscriber<? super PooledConnection<I, O>> subscriber;
        private final PooledConnection<I, O> pooledConnection;

        public ConnectionReuseEvent(Subscriber<? super PooledConnection<I, O>> subscriber,
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
