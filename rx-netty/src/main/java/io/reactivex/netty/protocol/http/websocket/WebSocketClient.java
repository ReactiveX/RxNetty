package io.reactivex.netty.protocol.http.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
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
        public Subscriber<ObservableConnection<T, T>> call(final Subscriber<? super ObservableConnection<T, T>> subscriber) {
            return new Subscriber<ObservableConnection<T, T>>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }

                @Override
                public void onNext(final ObservableConnection<T, T> connection) {
                    ChannelHandlerContext ctx = connection.getChannelHandlerContext();
                    final ChannelPipeline p = ctx.channel().pipeline();
                    ChannelHandlerContext hctx = p.context(WebSocketClientHandler.class);
                    if (hctx != null) {
                        WebSocketClientHandler handler = p.get(WebSocketClientHandler.class);
                        handler.handshakeFuture().addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                subscriber.onNext(connection);
                                subscriber.onCompleted();
                            }
                        });
                    }
                }
            };
        }
    }
}
