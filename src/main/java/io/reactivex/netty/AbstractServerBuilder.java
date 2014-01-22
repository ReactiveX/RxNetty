package io.reactivex.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractServerBuilder<I, O, B extends AbstractServerBuilder, S extends NettyServer<I, O>> {

    protected final int port;
    protected final ServerBootstrap serverBootstrap;
    protected Class<? extends ServerChannel> serverChannelClass;
    protected NettyPipelineConfigurator pipelineConfigurator;

    protected AbstractServerBuilder(int port) {
        serverBootstrap = new ServerBootstrap();
        this.port = port;
        serverChannelClass = NioServerSocketChannel.class;
    }

    protected AbstractServerBuilder(ServerBootstrap bootstrap, int port) {
        serverBootstrap = bootstrap;
        this.port = port;
        serverChannelClass = NioServerSocketChannel.class;
    }

    public B eventLoops(EventLoopGroup acceptorGroup, EventLoopGroup workerGroup) {
        serverBootstrap.group(acceptorGroup, workerGroup);
        return returnBuilder();
    }

    public B eventLoop(EventLoopGroup singleGroup) {
        serverBootstrap.group(singleGroup);
        return returnBuilder();
    }

    public B channel(Class<ServerChannel> serverChannelClass) {
        this.serverChannelClass = serverChannelClass;
        return returnBuilder();
    }

    public <T> B channelOption(ChannelOption<T> option, T value) {
        serverBootstrap.option(option, value);
        return returnBuilder();
    }

    public B defaultChannelOptions() {
        channelOption(ChannelOption.SO_KEEPALIVE, true);
        return returnBuilder();
    }

    public B pipelineConfigurator(NettyPipelineConfigurator pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return returnBuilder();
    }

    public S build() {
        if (null == serverChannelClass) {
            serverChannelClass = NioServerSocketChannel.class;
            EventLoopGroup acceptorGroup = serverBootstrap.group();
            if (null == acceptorGroup) {
                serverBootstrap.group(new NioEventLoopGroup());
            }
        }

        if (null == serverBootstrap.group()) {
            if (NioServerSocketChannel.class == serverChannelClass) {
                serverBootstrap.group(new NioEventLoopGroup());
            } else {
                // Fail fast for defaults we do not support.
                throw new IllegalStateException("Specified a channel class but not the event loop group.");
            }
        }

        serverBootstrap.channel(serverChannelClass);
        return createServer();
    }

    protected abstract S createServer();

    @SuppressWarnings("unchecked")
    protected B returnBuilder() {
        return (B) this;
    }
}
