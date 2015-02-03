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
 */
package io.reactivex.netty.client;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;

import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author Nitesh Kant
 */
public class NioClientEventLoopGroup extends NioEventLoopGroup {

    public NioClientEventLoopGroup() {
        this(0);
    }

    public NioClientEventLoopGroup(int nThreads) {
        this(nThreads, (Executor) null);
    }

    public NioClientEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SelectorProvider.provider());
    }

    public NioClientEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }

    public NioClientEventLoopGroup(int nThreads, ThreadFactory threadFactory,
                                   SelectorProvider selectorProvider) {
        super(nThreads, threadFactory, selectorProvider);
        Set<EventExecutor> children = children();
        for (EventExecutor child : children) {
            if (child instanceof FastThreadLocal) {

            }
        }
    }

    public NioClientEventLoopGroup(int nThreads, Executor executor,
                                   SelectorProvider selectorProvider) {
        super(nThreads, executor, selectorProvider);
    }

    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        return super.newChild(executor, args);
    }

    @Override
    public EventLoop next() {
        return super.next();
    }
}
