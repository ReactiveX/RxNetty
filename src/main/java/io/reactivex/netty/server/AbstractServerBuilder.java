package io.reactivex.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.reactivex.netty.ConnectionHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractServerBuilder<I, O, B extends AbstractServerBuilder, S extends RxServer<I, O>> {

    protected final int port;
    protected final ServerBootstrap serverBootstrap;
    protected final ConnectionHandler<I, O> connectionHandler;
    protected Class<? extends ServerChannel> serverChannelClass;
    protected PipelineConfigurator<I, O> pipelineConfigurator;

    protected AbstractServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        this(port, connectionHandler, new ServerBootstrap());
    }

    protected AbstractServerBuilder(int port, ConnectionHandler<I, O> connectionHandler, ServerBootstrap bootstrap) {
        if (null == connectionHandler) {
            throw new IllegalArgumentException("Connection handler can not be null");
        }
        if (null == bootstrap) {
            throw new IllegalArgumentException("Server bootstrap can not be null");
        }
        serverBootstrap = bootstrap;
        this.port = port;
        this.connectionHandler = connectionHandler;
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

    public B pipelineConfigurator(PipelineConfigurator<I, O> pipelineConfigurator) {
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
