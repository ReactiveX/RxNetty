package io.reactivex.netty.channel;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * An implementation of {@link RxEventLoopProvider} that returns the same {@link EventLoopGroup} instance for both
 * client and server.
 *
 * @author Nitesh Kant
 */
public class SingleNioLoopProvider implements RxEventLoopProvider {

    private final EventLoopGroup eventLoop = new NioEventLoopGroup(0/*means default in netty*/,
                                                                   new RxDefaultThreadFactory("rx-selector"));

    @Override
    public EventLoopGroup globalClientEventLoop() {
        return eventLoop;
    }

    @Override
    public EventLoopGroup globalServerEventLoop() {
        return eventLoop;
    }
}
