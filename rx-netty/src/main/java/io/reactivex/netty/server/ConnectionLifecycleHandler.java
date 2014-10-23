/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.Subscriber;

public class ConnectionLifecycleHandler<I, O> extends ChannelInboundHandlerAdapter {

    private final ConnectionHandler<I, O> connectionHandler;
    private final ErrorHandler errorHandler;
    private final ObservableConnectionFactory<I, O> connectionFactory;
    private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;
    private ObservableConnection<I,O> connection;

    public ConnectionLifecycleHandler(ConnectionHandler<I, O> connectionHandler,
                                      ObservableConnectionFactory<I, O> connectionFactory,
                                      ErrorHandler errorHandler,
                                      MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
        this.connectionHandler = connectionHandler;
        this.connectionFactory = connectionFactory;
        this.eventsSubject = eventsSubject;
        this.errorHandler = null == errorHandler ? new DefaultErrorHandler() : errorHandler;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (null != connection) {
            connection.close();
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(null == ctx.channel().pipeline().get(SslHandler.class)) {
            final long startTimeMillis = Clock.newStartTimeMillis();
            connection = connectionFactory.newConnection(ctx.channel());
            eventsSubject.onEvent(ServerMetricsEvent.NEW_CLIENT_CONNECTED);

            super.channelActive(ctx); // Called before connection handler call to finish the pipeline before the connection
            // is handled.

            handleConnection(startTimeMillis);
        } else {
            super.channelActive(ctx);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof SslHandshakeCompletionEvent) {
            final long startTimeMillis = Clock.newStartTimeMillis();
            connection = connectionFactory.newConnection(ctx.channel());
            handleConnection(startTimeMillis);
        }
    }

    private void handleConnection(final long startTimeMillis) {
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

        handledObservable.subscribe(new Subscriber<Void>() {
            @Override
            public void onCompleted() {
                eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_HANDLING_SUCCESS,
                                      Clock.onEndMillis(startTimeMillis));
                connection.close();
            }

            @Override
            public void onError(Throwable e) {
                invokeErrorHandler(e);
                eventsSubject.onEvent(ServerMetricsEvent.CONNECTION_HANDLING_FAILED,
                                      Clock.onEndMillis(startTimeMillis), e);
                connection.close();
            }

            @Override
            public void onNext(Void aVoid) {
                // No Op.
            }
        });
    }

    private void invokeErrorHandler(Throwable throwable) {
        try {
            errorHandler.handleError(throwable);
        } catch (Exception e) {
            System.err.println("Error while invoking error handler. Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
