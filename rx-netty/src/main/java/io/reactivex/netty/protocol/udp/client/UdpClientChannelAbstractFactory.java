package io.reactivex.netty.protocol.udp.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.client.ClientChannelAbstractFactory;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.RxClient;

import java.net.InetSocketAddress;

/**
 * @author Nitesh Kant
 */
public class UdpClientChannelAbstractFactory<I, O> implements ClientChannelAbstractFactory<I, O> {

    @Override
    public ClientChannelFactory<I, O> newClientChannelFactory(RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
                                                              ObservableConnectionFactory<I, O> connectionFactory) {
        final InetSocketAddress receiverAddress = new InetSocketAddress(serverInfo.getHost(), serverInfo.getPort());
        return new ClientChannelFactoryImpl<I, O>(clientBootstrap,
                                                  new UdpClientConnectionFactory<I, O>(receiverAddress), serverInfo);
    }
}
