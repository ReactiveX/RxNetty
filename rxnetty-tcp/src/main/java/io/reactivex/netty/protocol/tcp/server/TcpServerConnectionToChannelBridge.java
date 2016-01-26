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
package io.reactivex.netty.protocol.tcp.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.channel.AbstractConnectionToChannelBridge;
import io.reactivex.netty.channel.ChannelSubscriberEvent;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.channel.EmitConnectionEvent;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.events.EventAttributeKeys;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;

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
    private final ChannelSubscriberEvent<R, W> channelSubscriberEvent;

    private TcpServerConnectionToChannelBridge(ConnectionHandler<R, W> connectionHandler,
                                               TcpServerEventPublisher eventPublisher, boolean isSecure) {
        super(HANDLER_NAME, eventPublisher, eventPublisher);
        this.connectionHandler = connectionHandler;
        this.eventPublisher = eventPublisher;
        this.isSecure = isSecure;
        channelSubscriberEvent = new ChannelSubscriberEvent<>(new NewChannelSubscriber());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        userEventTriggered(ctx, channelSubscriberEvent);
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

    private final class NewChannelSubscriber extends Subscriber<Channel> {

        @Override
        public void onCompleted() {
            // No Op.
        }

        @Override
        public void onError(Throwable e) {
            logger.error("Error while listening for new client connections.", e);
        }

        @Override
        public void onNext(final Channel channel) {
            channel.attr(EventAttributeKeys.EVENT_PUBLISHER).set(eventPublisher);
            channel.attr(EventAttributeKeys.CONNECTION_EVENT_LISTENER).set(eventPublisher);

            final Connection<R, W> connection = ConnectionImpl.fromChannel(channel);
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
                logger.error("Connection handler returned null.");
                handledObservable = Observable.empty();
            }

            handledObservable
                    .onErrorResumeNext(
                            new Func1<Throwable, Observable<? extends Void>>() {
                                @Override
                                public Observable<? extends Void> call(Throwable throwable) {
                                    if (throwable instanceof ClosedChannelException) {
                                        return Observable.empty();
                                    } else {
                                        /*Since, this is always reading input for new requests, it will always get a
                                        closed channel exception on connection close from client. No point in logging
                                        that error.*/
                                        if (eventPublisher.publishingEnabled()) {
                                            eventPublisher.onConnectionHandlingFailed(Clock.onEndNanos(startTimeNanos),
                                                                                      NANOSECONDS,
                                                                                      throwable);
                                        }
                                        logger.error("Error processing connection.", throwable);
                                        return connection.close();
                                    }
                                }
                            })
                    .ambWith(connection.closeListener())
                    .concatWith(connection.close())
                    .doOnCompleted(new Action0() {
                        @Override
                        public void call() {
                            if (eventPublisher.publishingEnabled()) {
                                eventPublisher.onConnectionHandlingSuccess(Clock.onEndNanos(startTimeNanos),
                                                                           NANOSECONDS);
                            }
                        }
                    })
                    .subscribe();
        }
    }
}
