package io.reactivex.netty.contexts.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Subscriber;

import static io.reactivex.netty.contexts.AbstractClientContextHandler.NewContextEvent;

/**
 * @author Nitesh Kant
 */
public class HttpContextClientChannelFactory<I, O> extends
        ClientChannelFactoryImpl<HttpClientResponse<O>, HttpClientRequest<I>> {

    private final RequestCorrelator correlator;

    public HttpContextClientChannelFactory(Bootstrap clientBootstrap, RequestCorrelator correlator) {
        super(clientBootstrap);
        this.correlator = correlator;
    }

    @Override
    public ChannelFuture connect(
            Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> subscriber,
            RxClient.ServerInfo serverInfo,
            ClientConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>,
                    ? extends ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionFactory) {
        final ContextCapturingSubscriber capturingSubscriber = new ContextCapturingSubscriber(subscriber);
        return super.connect(capturingSubscriber, serverInfo, connectionFactory);
    }

    @Override
    public void onNewConnection(ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> newConnection,
                                Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> subscriber) {
        /*
         * This will either be called after a call to connect() or directly (from pool).
         * If it is the former then the subscriber should already be a capturing sub. In case of latter, we should be
         * called from the thread that has the relevant state & hence we capture the context now.
         */
        final Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> subToUse;
        if (ContextCapturingSubscriber.class == subscriber.getClass()) {
            subToUse = subscriber;
        } else {
            subToUse = new ContextCapturingSubscriber(subscriber);
        }

        super.onNewConnection(newConnection, subToUse);
    }

    private class ContextCapturingSubscriber extends Subscriber<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> {

        private final Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> original;
        private final String requestId;
        private final ContextsContainer container;

        private ContextCapturingSubscriber(Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> original) {
            super(original);
            this.original = original;
            requestId = correlator.getRequestIdForClientRequest();
            container = correlator.getContextForClientRequest(requestId);
        }

        @Override
        public void onCompleted() {
            original.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            original.onError(e);
        }

        @Override
        public void onNext(ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> connection) {
            if (null != requestId && null != container) {
                connection.getChannelHandlerContext().pipeline()
                          .fireUserEventTriggered(new NewContextEvent(requestId, container));
            }
            original.onNext(connection);
        }
    }
}
