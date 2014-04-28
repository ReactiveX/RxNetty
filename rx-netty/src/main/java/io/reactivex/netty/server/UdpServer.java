package io.reactivex.netty.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * A UDP/IP server.
 *
 * @author Nitesh Kant
 */
public class UdpServer<I, O> extends AbstractServer<I, O, Bootstrap, Channel, UdpServer<I, O>> {

    public UdpServer(Bootstrap bootstrap, int port, final ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, null, connectionHandler);
    }

    public UdpServer(Bootstrap bootstrap, int port, final PipelineConfigurator<I, O> pipelineConfigurator,
                    final ConnectionHandler<I, O> connectionHandler) {
        super(bootstrap, port);
        bootstrap.handler(newChannelInitializer(pipelineConfigurator, connectionHandler));
    }
}
