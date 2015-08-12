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
package io.reactivex.netty.protocol.tcp.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.channel.AbstractConnectionToChannelBridge;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionSubscriberEvent;
import io.reactivex.netty.channel.EmitConnectionEvent;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import java.nio.channels.ClosedChannelException;

import static java.util.concurrent.TimeUnit.*;

/**
 * An implementation of {@link AbstractConnectionToChannelBridge} for servers.
 *
 * @param <R> The type of objects read from the server using this bridge.
 * @param <W> The type of objects written to this server using this bridge.
 */
public class TcpServerConnectionToChannelBridge<R, W> extends AbstractConnectionToChannelBridge<R, W> {

    private static final Logger logger = LoggerFactory.getLogger(TcpServerConnectionToChannelBridge.class);
    private static final String HANDLER_NAME = "server-conn-channel-bridge";

    private final ConnectionHandler<R, W> connectionHandler;
    private final TcpServerEventPublisher eventPublisher;
    private final boolean isSecure;
    private final ConnectionSubscriberEvent<R, W> connectionSubscriberEvent;

    private TcpServerConnectionToChannelBridge(ConnectionHandler<R, W> connectionHandler,
                                               TcpServerEventPublisher eventPublisher, boolean isSecure) {
        super(HANDLER_NAME, eventPublisher, eventPublisher);
        this.connectionHandler = connectionHandler;
        this.eventPublisher = eventPublisher;
        this.isSecure = isSecure;
        connectionSubscriberEvent = new ConnectionSubscriberEvent<>(new NewConnectionSubscriber());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        userEventTriggered(ctx, connectionSubscriberEvent);
        if (!isSecure) {/*When secure, the event is triggered post SSL handshake via the SslCodec*/
            userEventTriggered(ctx, EmitConnectionEvent.INSTANCE);
        }
        super.channelRegistered(ctx);
    }

    public static <R, W> TcpServerConnectionToChannelBridge<R, W> addToPipeline(ChannelPipeline pipeline,
                                                                             ConnectionHandler<R, W> connectionHandler,
                                                                             TcpServerEventPublisher eventPublisher,
                                                                             boolean isSecure) {
        TcpServerConnectionToChannelBridge<R, W> toAdd = new TcpServerConnectionToChannelBridge<>(connectionHandler,
                                                                                            eventPublisher, isSecure);
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
            final long startTimeNanos = eventPublisher.publishingEnabled() ? Clock.newStartTimeNanos() : -1;
            if (eventPublisher.publishingEnabled()) {
                eventPublisher.onNewClientConnected();
            }
            Observable<Void> handledObservable;
            try {
                if (eventPublisher.publishingEnabled()) {
                    eventPublisher.onConnectionHandlingStart(Clock.onEndNanos(startTimeNanos), NANOSECONDS);
                }
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
                    if (eventPublisher.publishingEnabled()) {
                        eventPublisher.onConnectionHandlingSuccess(Clock.onEndNanos(startTimeNanos), NANOSECONDS);
                    }
                    connection.closeNow();
                }

                @Override
                public void onError(Throwable e) {
                    if (!(e instanceof ClosedChannelException)) {
                        if (eventPublisher.publishingEnabled()) {
                            eventPublisher.onConnectionHandlingFailed(Clock.onEndNanos(startTimeNanos), NANOSECONDS,
                                                                      e);
                        }
                        /*Since, this is always reading input for new requests, it will always get a closed channel
                        exception on connection close from client. No point in logging that error.*/
                        logger.error("Error processing connection.", e);
                    }
                    connection.closeNow();
                }

                @Override
                public void onNext(Void aVoid) {
                    // No Op.
                }
            });

            connection.unsafeNettyChannel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    handlingSubscription.unsubscribe(); // Cancel on connection close.
                }
            });
        }
    }
}
