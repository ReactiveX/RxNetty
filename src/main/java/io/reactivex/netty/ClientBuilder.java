package io.reactivex.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * A builder to build an instance of {@link NettyClient}
 *
 * @author Nitesh Kant
 */
public class ClientBuilder<I, O> {

    private final NettyClient.ServerInfo serverInfo;
    private final Bootstrap bootstrap;
    private NettyPipelineConfigurator pipelineConfigurator;
    private Class<? extends SocketChannel> socketChannel;
    private EventLoopGroup eventLoopGroup;

    public ClientBuilder(String host, int port) {
        this(host, port, new Bootstrap());
    }

    public ClientBuilder(String host, int port, Bootstrap bootstrap) {
        serverInfo = new NettyClient.ServerInfo(host, port);
        this.bootstrap = bootstrap;
    }

    public ClientBuilder<I, O> defaultChannelOptions() {
        return channelOption(ChannelOption.TCP_NODELAY, true);
    }

    public ClientBuilder<I, O> pipelineConfigurator(NettyPipelineConfigurator pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return this;
    }

    public <T> ClientBuilder<I, O> channelOption(ChannelOption<T> option, T value) {
        bootstrap.option(option, value);
        return this;
    }

    public ClientBuilder<I, O> channel(Class<? extends SocketChannel> socketChannel) {
        this.socketChannel = socketChannel;
        return this;
    }

    public ClientBuilder<I, O> eventloop(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return this;
    }

    public NettyClient<I, O> build() {
        if (null == socketChannel) {
            socketChannel = NioSocketChannel.class;
            if (null == eventLoopGroup) {
                eventLoopGroup = new NioEventLoopGroup();
            }
        }

        if (null == eventLoopGroup) {
            if (NioSocketChannel.class == socketChannel) {
                eventLoopGroup = new NioEventLoopGroup();
            } else {
                // Fail fast for defaults we do not support.
                throw new IllegalStateException("Specified a channel class but not the event loop group.");
            }
        }

        bootstrap.channel(socketChannel).group(eventLoopGroup);
        return new NettyClient<I, O>(serverInfo, bootstrap, pipelineConfigurator);
    }
}
