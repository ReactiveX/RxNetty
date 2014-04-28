package io.reactivex.netty.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.reactivex.netty.channel.ConnectionHandler;

/**
 * @author Nitesh Kant
 */
public class UdpServerBuilder<I, O> extends AbstractServerBuilder<I, O, Bootstrap, Channel, UdpServerBuilder<I, O>,
        UdpServer<I, O>> {

    public UdpServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        this(port, connectionHandler, new Bootstrap());
    }

    public UdpServerBuilder(int port, ConnectionHandler<I, O> connectionHandler, Bootstrap bootstrap) {
        super(port, bootstrap, connectionHandler);
    }

    @Override
    protected Class<? extends Channel> defaultServerChannelClass() {
        return NioDatagramChannel.class;
    }

    @Override
    public UdpServerBuilder<I, O> defaultChannelOptions() {
        channelOption(ChannelOption.SO_BROADCAST, true);
        return super.defaultChannelOptions();
    }

    @Override
    protected UdpServer<I, O> createServer() {
        if (null != pipelineConfigurator) {
            return new UdpServer<I, O>(serverBootstrap, port, pipelineConfigurator, connectionHandler);
        } else {
            return new UdpServer<I, O>(serverBootstrap, port, connectionHandler);
        }
    }
}
