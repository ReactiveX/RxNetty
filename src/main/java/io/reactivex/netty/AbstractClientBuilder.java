package io.reactivex.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractClientBuilder<I, O, B extends AbstractClientBuilder, C extends NettyClient<I, O>> {

    protected final NettyClient.ServerInfo serverInfo;
    protected final Bootstrap bootstrap;
    protected NettyPipelineConfigurator pipelineConfigurator;
    protected Class<? extends SocketChannel> socketChannel;
    protected EventLoopGroup eventLoopGroup;

    protected AbstractClientBuilder(Bootstrap bootstrap, String host, int port) {
        this.bootstrap = bootstrap;
        serverInfo = new NettyClient.ServerInfo(host, port);
    }

    protected AbstractClientBuilder(String host, int port) {
        this(new Bootstrap(), host, port);
    }

    public B defaultChannelOptions() {
        channelOption(ChannelOption.SO_KEEPALIVE, true);
        return channelOption(ChannelOption.TCP_NODELAY, true);
    }

    public B pipelineConfigurator(NettyPipelineConfigurator pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return returnBuilder();
    }

    public <T> B channelOption(ChannelOption<T> option, T value) {
        bootstrap.option(option, value);
        return returnBuilder();
    }

    public B channel(Class<? extends SocketChannel> socketChannel) {
        this.socketChannel = socketChannel;
        return returnBuilder();
    }

    public B eventloop(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return returnBuilder();
    }

    public C build() {
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
        return createClient();
    }

    protected abstract C createClient();

    @SuppressWarnings("unchecked")
    protected B returnBuilder() {
        return (B) this;
    }
}
