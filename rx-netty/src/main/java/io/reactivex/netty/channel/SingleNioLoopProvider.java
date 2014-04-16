package io.reactivex.netty.channel;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link RxEventLoopProvider} that returns the same {@link EventLoopGroup} instance for both
 * client and server.
 *
 * @author Nitesh Kant
 */
public class SingleNioLoopProvider implements RxEventLoopProvider {

    private final SharedNioEventLoopGroup eventLoop = new SharedNioEventLoopGroup();

    @Override
    public EventLoopGroup globalClientEventLoop() {
        eventLoop.retain();
        return eventLoop;
    }

    @Override
    public EventLoopGroup globalServerEventLoop() {
        eventLoop.retain();
        return eventLoop;
    }

    private static class SharedNioEventLoopGroup extends NioEventLoopGroup {

        private final AtomicInteger refCount = new AtomicInteger();

        public SharedNioEventLoopGroup() {
            super(0, new RxDefaultThreadFactory("rx-selector"));
        }

        @Override
        public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
            if (0 == release()) {
                return super.shutdownGracefully(quietPeriod, timeout, unit);
            } else {
                return terminationFuture();
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public void shutdown() {
            if (0 == release()) {
                super.shutdown();
            }
        }

        public int retain() {
            return refCount.incrementAndGet();
        }

        public int release() {
            return refCount.decrementAndGet();
        }
    }
}
