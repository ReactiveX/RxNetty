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
package io.reactivex.netty.protocol.http.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPipelineException;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;

/**
 * {@link WebSocketClient} delays connection handling to application subscriber
 * until the WebSocket handshake is complete.
 *
 * @author Tomasz Bak
 */
public class WebSocketClient<I extends WebSocketFrame, O extends WebSocketFrame> extends RxClientImpl<I, O> {

    @SuppressWarnings("rawtypes")
    private static final HandshakeOperator HANDSHAKE_OPERATOR = new HandshakeOperator();

    public WebSocketClient(String name, ServerInfo serverInfo, Bootstrap clientBootstrap,
                           PipelineConfigurator<O, I> pipelineConfigurator,
                           ClientConfig clientConfig, ClientChannelFactory<O, I> channelFactory,
                           ClientConnectionFactory<O, I, ? extends ObservableConnection<O, I>> connectionFactory,
                           MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        super(name, serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, channelFactory, connectionFactory, eventsSubject);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<ObservableConnection<O, I>> connect() {
        return super.connect().lift(HANDSHAKE_OPERATOR);
    }

    static class HandshakeOperator<T extends WebSocketFrame> implements Operator<ObservableConnection<T, T>, ObservableConnection<T, T>> {
        @Override
        public Subscriber<ObservableConnection<T, T>> call(final Subscriber<? super ObservableConnection<T, T>> originalSubscriber) {
            Subscriber<ObservableConnection<T, T>> liftSubscriber = new Subscriber<ObservableConnection<T, T>>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    originalSubscriber.onError(e);
                }

                @Override
                public void onNext(final ObservableConnection<T, T> connection) {
                    final ChannelPipeline p = connection.getChannel().pipeline();
                    ChannelHandlerContext hctx = p.context(WebSocketClientHandler.class);
                    if (hctx != null) {
                        WebSocketClientHandler handler = p.get(WebSocketClientHandler.class);
                        handler.addHandshakeFinishedListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                originalSubscriber.onNext(connection);
                                originalSubscriber.onCompleted();
                            }
                        });
                    } else {
                        originalSubscriber.onError(new ChannelPipelineException(
                                "invalid pipeline configuration - WebSocket pipeline with no WebSocketClientHandler"));
                    }
                }
            };
            originalSubscriber.add(liftSubscriber);
            return liftSubscriber;
        }
    }
}
