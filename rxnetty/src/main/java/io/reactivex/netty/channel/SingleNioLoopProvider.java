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
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.reactivex.netty.RxNetty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link RxEventLoopProvider} that returns the same {@link EventLoopGroup} instance for both
 * client and server.
 *
 * @author Nitesh Kant
 */
public class SingleNioLoopProvider extends RxEventLoopProvider {

    private final SharedNioEventLoopGroup eventLoop;
    private final SharedNioEventLoopGroup parentEventLoop;
    private final AtomicReference<EpollEventLoopGroup> nativeEventLoop;
    private final AtomicReference<EpollEventLoopGroup> nativeParentEventLoop;
    private final int parentEventLoopCount;
    private final int childEventLoopCount;

    public SingleNioLoopProvider() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public SingleNioLoopProvider(int threadCount) {
        eventLoop = new SharedNioEventLoopGroup(threadCount);
        parentEventLoop = eventLoop;
        parentEventLoopCount = childEventLoopCount = threadCount;
        nativeEventLoop = new AtomicReference<EpollEventLoopGroup>();
        nativeParentEventLoop = nativeEventLoop;
    }

    public SingleNioLoopProvider(int parentEventLoopCount, int childEventLoopCount) {
        this.parentEventLoopCount = parentEventLoopCount;
        this.childEventLoopCount = childEventLoopCount;
        parentEventLoop = new SharedNioEventLoopGroup(parentEventLoopCount);
        eventLoop = new SharedNioEventLoopGroup(childEventLoopCount);
        nativeParentEventLoop = new AtomicReference<EpollEventLoopGroup>();
        nativeEventLoop = new AtomicReference<EpollEventLoopGroup>();
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

    @Override
    public EventLoopGroup globalClientEventLoop(boolean nativeTransport) {
        if (nativeTransport && RxNetty.isUsingNativeTransport()) {
            return getNativeEventLoop();
        }
        return globalClientEventLoop();
    }

    @Override
    public EventLoopGroup globalServerEventLoop(boolean nativeTransport) {
        if (nativeTransport && RxNetty.isUsingNativeTransport()) {
            return getNativeEventLoop();
        }
        return globalServerEventLoop();
    }

    @Override
    public EventLoopGroup globalServerParentEventLoop(boolean nativeTransport) {
        if (nativeTransport && RxNetty.isUsingNativeTransport()) {
            return getNativeParentEventLoop();
        }
        return globalServerParentEventLoop();
    }

    private EpollEventLoopGroup getNativeParentEventLoop() {
        if (nativeParentEventLoop == nativeEventLoop) { // Means using same event loop for acceptor and worker pool.
            return getNativeEventLoop();
        }

        EpollEventLoopGroup eventLoopGroup = nativeParentEventLoop.get();
        if (null == eventLoopGroup) {
            EpollEventLoopGroup newEventLoopGroup = new EpollEventLoopGroup(parentEventLoopCount,
                                                                            new RxDefaultThreadFactory("rxnetty-epoll-eventloop"));
            if (!nativeParentEventLoop.compareAndSet(null, newEventLoopGroup)) {
                newEventLoopGroup.shutdownGracefully();
            }
        }
        return nativeParentEventLoop.get();
    }

    private EpollEventLoopGroup getNativeEventLoop() {
        EpollEventLoopGroup eventLoopGroup = nativeEventLoop.get();
        if (null == eventLoopGroup) {
            EpollEventLoopGroup newEventLoopGroup = new EpollEventLoopGroup(childEventLoopCount,
                                                                            new RxDefaultThreadFactory("rxnetty-epoll-eventloop"));
            if (!nativeEventLoop.compareAndSet(null, newEventLoopGroup)) {
                newEventLoopGroup.shutdownGracefully();
            }
        }
        return nativeEventLoop.get();
    }

    public static class SharedNioEventLoopGroup extends NioEventLoopGroup {

        private final AtomicInteger refCount = new AtomicInteger();

        public SharedNioEventLoopGroup() {
            super(0, new RxDefaultThreadFactory("rxnetty-nio-eventloop"));
        }

        public SharedNioEventLoopGroup(int threadCount) {
            super(threadCount, new RxDefaultThreadFactory("rxnetty-nio-eventloop"));
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
