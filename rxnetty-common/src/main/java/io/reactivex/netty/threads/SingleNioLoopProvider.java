/*
 * Copyright 2015 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.threads;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.reactivex.netty.RxNetty;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link RxEventLoopProvider} that returns the same {@link EventLoopGroup} instance for both
 * client and server.
 */
public class SingleNioLoopProvider extends RxEventLoopProvider {

    private final EventLoopGroup eventLoop;
    private final EventLoopGroup clientEventLoop;
    private final EventLoopGroup parentEventLoop;
    private final AtomicReference<EventLoopGroup> nativeEventLoop;
    private final AtomicReference<EventLoopGroup> nativeClientEventLoop;
    private final AtomicReference<EventLoopGroup> nativeParentEventLoop;
    private final int parentEventLoopCount;
    private final int childEventLoopCount;

    public SingleNioLoopProvider() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public SingleNioLoopProvider(int threadCount) {
        eventLoop = new NioEventLoopGroup(threadCount, new RxDefaultThreadFactory("rxnetty-nio-eventloop"));
        clientEventLoop = new PreferCurrentEventLoopGroup(eventLoop);
        parentEventLoop = eventLoop;
        parentEventLoopCount = childEventLoopCount = threadCount;
        nativeEventLoop = new AtomicReference<>();
        nativeClientEventLoop = new AtomicReference<>();
        nativeParentEventLoop = nativeEventLoop;
    }

    public SingleNioLoopProvider(int parentEventLoopCount, int childEventLoopCount) {
        this.parentEventLoopCount = parentEventLoopCount;
        this.childEventLoopCount = childEventLoopCount;
        parentEventLoop = new NioEventLoopGroup(parentEventLoopCount,
                                                new RxDefaultThreadFactory("rxnetty-nio-eventloop"));
        eventLoop = new NioEventLoopGroup(childEventLoopCount, new RxDefaultThreadFactory("rxnetty-nio-eventloop"));
        clientEventLoop = new PreferCurrentEventLoopGroup(eventLoop);
        nativeParentEventLoop = new AtomicReference<>();
        nativeEventLoop = new AtomicReference<>();
        nativeClientEventLoop = new AtomicReference<>();
    }

    @Override
    public EventLoopGroup globalClientEventLoop() {
        return clientEventLoop;
    }

    @Override
    public EventLoopGroup globalServerEventLoop() {
        return eventLoop;
    }

    @Override
    public EventLoopGroup globalServerParentEventLoop() {
        return parentEventLoop;
    }

    @Override
    public EventLoopGroup globalClientEventLoop(boolean nativeTransport) {
        if (nativeTransport && RxNetty.isUsingNativeTransport()) {
            return getNativeClientEventLoop();
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

    private EventLoopGroup getNativeParentEventLoop() {
        if (nativeParentEventLoop == nativeEventLoop) { // Means using same event loop for acceptor and worker pool.
            return getNativeEventLoop();
        }

        EventLoopGroup eventLoopGroup = nativeParentEventLoop.get();
        if (null == eventLoopGroup) {
            EventLoopGroup newEventLoopGroup = new EpollEventLoopGroup(parentEventLoopCount,
                                                                       new RxDefaultThreadFactory( "rxnetty-epoll-eventloop"));
            if (!nativeParentEventLoop.compareAndSet(null, newEventLoopGroup)) {
                newEventLoopGroup.shutdownGracefully();
            }
        }
        return nativeParentEventLoop.get();
    }

    private EventLoopGroup getNativeEventLoop() {
        EventLoopGroup eventLoopGroup = nativeEventLoop.get();
        if (null == eventLoopGroup) {
            EventLoopGroup newEventLoopGroup = new EpollEventLoopGroup(childEventLoopCount,
                                                                       new RxDefaultThreadFactory( "rxnetty-epoll-eventloop"));
            if (!nativeEventLoop.compareAndSet(null, newEventLoopGroup)) {
                newEventLoopGroup.shutdownGracefully();
            }
        }
        return nativeEventLoop.get();
    }

    private EventLoopGroup getNativeClientEventLoop() {
        EventLoopGroup eventLoopGroup = nativeClientEventLoop.get();
        if (null == eventLoopGroup) {
            EventLoopGroup newEventLoopGroup = new EpollEventLoopGroup(childEventLoopCount,
                                                                       new RxDefaultThreadFactory( "rxnetty-epoll-eventloop"));
            newEventLoopGroup = new PreferCurrentEventLoopGroup(newEventLoopGroup);
            if (!nativeClientEventLoop.compareAndSet(null, newEventLoopGroup)) {
                newEventLoopGroup.shutdownGracefully();
            }
        }
        return nativeClientEventLoop.get();
    }
}
