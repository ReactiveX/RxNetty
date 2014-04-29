package io.reactivex.netty.protocol.udp.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.pipeline.PipelineConfigurator;

import java.net.InetSocketAddress;

/**
 * An implementation of {@link RxClient} for UDP/IP
 *
 * @author Nitesh Kant
 */
public class UdpClient<I, O> extends RxClientImpl<I, O> {

    public UdpClient(ServerInfo serverInfo, Bootstrap clientBootstrap, ClientConfig clientConfig) {
        this(serverInfo, clientBootstrap, null, clientConfig);
    }

    public UdpClient(ServerInfo serverInfo, Bootstrap clientBootstrap, PipelineConfigurator<O, I> pipelineConfigurator,
                     ClientConfig clientConfig) {
        super(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig);
    }

    @Override
    protected ClientChannelFactory<O, I> _newChannelFactory(ServerInfo serverInfo, Bootstrap clientBootstrap,
                                                            ObservableConnectionFactory<O, I> connectionFactory) {
        final InetSocketAddress receiverAddress = new InetSocketAddress(serverInfo.getHost(), serverInfo.getPort());
        return new ClientChannelFactoryImpl<O, I>(clientBootstrap,
                                                  new UdpClientConnectionFactory<O, I>(receiverAddress), serverInfo);
    }
}
