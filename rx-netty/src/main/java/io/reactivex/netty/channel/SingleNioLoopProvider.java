/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    private final SharedNioEventLoopGroup eventLoop;
    private final SharedNioEventLoopGroup parentEventLoop;

    public SingleNioLoopProvider() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public SingleNioLoopProvider(int threadCount) {
        eventLoop = new SharedNioEventLoopGroup(threadCount);
        parentEventLoop = eventLoop;
    }

    public SingleNioLoopProvider(int parentEventLoopCount, int childEventLoopCount) {
        eventLoop = new SharedNioEventLoopGroup(childEventLoopCount);
        parentEventLoop = new SharedNioEventLoopGroup(parentEventLoopCount);
    }

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

    @Override
    public EventLoopGroup globalServerParentEventLoop() {
        return parentEventLoop;
    }

    public static class SharedNioEventLoopGroup extends NioEventLoopGroup {

        private final AtomicInteger refCount = new AtomicInteger();

        public SharedNioEventLoopGroup() {
            super(0, new RxDefaultThreadFactory("rx-selector"));
        }

        public SharedNioEventLoopGroup(int threadCount) {
            super(threadCount, new RxDefaultThreadFactory("rx-selector"));
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
        @Deprecated
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
