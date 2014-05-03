package io.reactivex.netty.protocol.udp.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.client.ClientChannelAbstractFactory;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * An implementation of {@link RxClient} for UDP/IP
 *
 * @author Nitesh Kant
 */
public class UdpClient<I, O> extends RxClientImpl<I, O> {

    public UdpClient(ServerInfo serverInfo, Bootstrap clientBootstrap, ClientConfig clientConfig) {
        this(serverInfo, clientBootstrap, null, clientConfig, new UdpClientChannelAbstractFactory<O, I>());
    }

    public UdpClient(ServerInfo serverInfo, Bootstrap bootstrap, PipelineConfigurator<O, I> pipelineConfigurator,
                     ClientConfig clientConfig, ClientChannelAbstractFactory<O, I> clientChannelFactory) {
        super(serverInfo, bootstrap, pipelineConfigurator, clientConfig, clientChannelFactory);
    }
}
