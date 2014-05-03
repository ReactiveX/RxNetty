package io.reactivex.netty.protocol.http.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.client.ClientChannelAbstractFactory;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.RxClient;

/**
 * @author Nitesh Kant
 */
public class HttpClientChannelAbstractFactory<I, O>
        implements ClientChannelAbstractFactory<HttpClientResponse<O>, HttpClientRequest<I>> {

    @Override
    public ClientChannelFactory<HttpClientResponse<O>, HttpClientRequest<I>> newClientChannelFactory(
            RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
            ObservableConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>> connectionFactory) {
        return new HttpClientChannelFactory<I, O>(clientBootstrap, connectionFactory, serverInfo);
    }
}
