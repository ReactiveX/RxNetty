package io.reactivex.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * A convenience builder for creating instances of {@link NettyServer}
 *
 * @author Nitesh Kant
 */
public class ServerBuilder<I, O> {

    private final int port;
    private final ServerBootstrap serverBootstrap;
    private Class<? extends ServerChannel> serverChannelClass;
    private NettyPipelineConfigurator pipelineConfigurator;

    public ServerBuilder(int port) {
        this(port, new ServerBootstrap());
    }

    public ServerBuilder(int port, ServerBootstrap bootstrap) {
        this.port = port;
        serverBootstrap = bootstrap;
        serverChannelClass = NioServerSocketChannel.class;
    }

    public ServerBuilder<I, O> eventLoops(EventLoopGroup acceptorGroup, EventLoopGroup workerGroup) {
        serverBootstrap.group(acceptorGroup, workerGroup);
        return this;
    }

    public ServerBuilder<I, O> eventLoop(EventLoopGroup singleGroup) {
        serverBootstrap.group(singleGroup);
        return this;
    }

    public ServerBuilder<I, O> channel(Class<ServerChannel> serverChannelClass) {
        this.serverChannelClass = serverChannelClass;
        return this;
    }

    public <T> ServerBuilder<I, O> channelOption(ChannelOption<T> option, T value) {
        serverBootstrap.option(option, value);
        return this;
    }

    public ServerBuilder<I, O> defaultChannelOptions() {
        return this;
    }

    public ServerBuilder<I, O> pipelineConfigurator(NettyPipelineConfigurator pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return this;
    }

    public NettyServer<I, O> build() {
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
        return new NettyServer<I, O>(serverBootstrap, port, pipelineConfigurator);
    }
}
