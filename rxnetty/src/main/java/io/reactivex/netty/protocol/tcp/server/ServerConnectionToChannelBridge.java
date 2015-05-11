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
package io.reactivex.netty.protocol.tcp.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.tcp.AbstractConnectionToChannelBridge;
import io.reactivex.netty.protocol.tcp.ConnectionSubscriberEvent;
import io.reactivex.netty.protocol.tcp.EmitConnectionEvent;
import io.reactivex.netty.server.ServerChannelMetricEventProvider;
import io.reactivex.netty.server.ServerMetricsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Actions;

import java.nio.channels.ClosedChannelException;

/**
 * An implementation of {@link io.reactivex.netty.protocol.tcp.AbstractConnectionToChannelBridge} for servers.
 *
 * @param <R> The type of objects read from the server using this bridge.
 * @param <W> The type of objects written to this server using this bridge.
 *
 * @author Nitesh Kant
 */
public class ServerConnectionToChannelBridge<R, W> extends AbstractConnectionToChannelBridge<R, W> {

    private static final Logger logger = LoggerFactory.getLogger(ServerConnectionToChannelBridge.class);
    private static final String HANDLER_NAME = "server-conn-channel-bridge";

    private final ConnectionHandler<R, W> connectionHandler;
    private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;
    private final boolean isSecure;
    private final NewConnectionSubscriber newConnectionSubscriber;
    private final ConnectionSubscriberEvent<R, W> connectionSubscriberEvent;

    private ServerConnectionToChannelBridge(ConnectionHandler<R, W> connectionHandler,
                                           MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject, boolean isSecure) {
        super(HANDLER_NAME, eventsSubject, ServerChannelMetricEventProvider.INSTANCE);
        this.connectionHandler = connectionHandler;
        this.eventsSubject = eventsSubject;
        this.isSecure = isSecure;
        newConnectionSubscriber = new NewConnectionSubscriber();
        connectionSubscriberEvent = new ConnectionSubscriberEvent<>(newConnectionSubscriber);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        userEventTriggered(ctx, connectionSubscriberEvent);
        if (!isSecure) {/*When secure, the event is triggered post SSL handshake via the SslCodec*/
            userEventTriggered(ctx, EmitConnectionEvent.INSTANCE);
        }
        super.channelRegistered(ctx);
    }

    public static <R, W> ServerConnectionToChannelBridge<R, W> addToPipeline(ChannelPipeline pipeline,
                                                                             ConnectionHandler<R, W> connectionHandler,
                                                                             MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject,
                                                                             boolean isSecure) {
        ServerConnectionToChannelBridge<R, W> toAdd = new ServerConnectionToChannelBridge<>(connectionHandler,
                                                                                            eventsSubject, isSecure);
        pipeline.addLast(HANDLER_NAME, toAdd);
        return toAdd;
    }

    private final class NewConnectionSubscriber extends Subscriber<Connection<R, W>> {

        private Subscription handlingSubscription;

        @Override
        public void onCompleted() {
            // No Op.
        }

        @Override
        public void onError(Throwable e) {
            logger.error("Error while listening for new client connections.", e);
        }

        @Override
        public void onNext(final Connection<R, W> connection) {
            final long startTimeMillis = Clock.newStartTimeMillis();
            eventsSubject.onEvent(ServerMetricsEvent.NEW_CLIENT_CONNECTED);
            Observable<Void> handledObservable;
            try {
                eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_HANDLING_START, Clock.onEndMillis(startTimeMillis));
                handledObservable = connectionHandler.handle(connection);
            } catch (Throwable throwable) {
                handledObservable = Observable.error(throwable);
            }

            if (null == handledObservable) {
                handledObservable = Observable.empty();
            }

            handlingSubscription = handledObservable.subscribe(new Subscriber<Void>() {
                @Override
                public void onCompleted() {
                    eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_HANDLING_SUCCESS,
                                          Clock.onEndMillis(startTimeMillis));
                    closeConnectionNow();
                }

                @Override
                public void onError(Throwable e) {
                    eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_HANDLING_FAILED,
                                          Clock.onEndMillis(startTimeMillis), e);
                    if (!(e instanceof ClosedChannelException)) {
                        /*Since, this is always reading input for new requests, it will always get a closed channel
                        exception on connection close from client. No point in logging that error.*/
                        logger.error("Error processing connection.", e);
                    }
                    closeConnectionNow();
                }

                @Override
                public void onNext(Void aVoid) {
                    // No Op.
                }

                private void closeConnectionNow() {
                    connection.close()
                              .subscribe(Actions.empty(), new Action1<Throwable>() {
                                  @Override
                                  public void call(Throwable throwable) {
                                      logger.error("Error closing connection.", throwable);
                                  }
                              });
                }
            });

            connection.getNettyChannel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    handlingSubscription.unsubscribe(); // Cancel on connection close.
                }
            });
        }
    }
}
