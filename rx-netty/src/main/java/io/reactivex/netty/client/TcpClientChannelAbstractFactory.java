package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnectionFactory;

/**
 * @author Nitesh Kant
 */
public class TcpClientChannelAbstractFactory<I, O> implements ClientChannelAbstractFactory<O, I> {

    @Override
    public ClientChannelFactory<O, I> newClientChannelFactory(RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
                                                              ObservableConnectionFactory<O, I> connectionFactory) {
        return new ClientChannelFactoryImpl<O, I>(clientBootstrap, connectionFactory, serverInfo);
    }
}
