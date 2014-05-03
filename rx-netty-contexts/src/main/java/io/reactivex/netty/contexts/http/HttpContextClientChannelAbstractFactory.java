package io.reactivex.netty.contexts.http;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientConnectionHandler;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.protocol.http.client.HttpClientChannelAbstractFactory;
import io.reactivex.netty.protocol.http.client.HttpClientChannelFactory;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Subscriber;

import static io.reactivex.netty.contexts.AbstractClientContextHandler.NewContextEvent;

/**
 * @author Nitesh Kant
 */
public class HttpContextClientChannelAbstractFactory<I, O> extends HttpClientChannelAbstractFactory<I, O> {

    private final RequestCorrelator correlator;

    public HttpContextClientChannelAbstractFactory(RequestCorrelator correlator) {
        this.correlator = correlator;
    }

    @Override
    public ClientChannelFactory<HttpClientResponse<O>, HttpClientRequest<I>> newClientChannelFactory(
            RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
            ObservableConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>> connectionFactory) {
        return new HttpClientChannelFactory<I, O>(clientBootstrap, connectionFactory, serverInfo) {

            @Override
            public ClientConnectionHandler<HttpClientResponse<O>, HttpClientRequest<I>> newConnectionHandler(
                    Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> subscriber) {

                final String requestId = correlator.getRequestIdForClientRequest();
                final ContextsContainer container = correlator.getContextForClientRequest(requestId);

                return new ClientConnectionHandler<HttpClientResponse<O>, HttpClientRequest<I>>(subscriber) {
                    @Override
                    protected void onNewConnection(ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> newConnection) {
                        if (null != requestId && null != container) {
                            newConnection.getChannelHandlerContext().pipeline()
                                         .fireUserEventTriggered(new NewContextEvent(requestId, container));
                        }
                        super.onNewConnection(newConnection);
                    }
                };
            }
        };
    }
}
