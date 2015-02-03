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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This {@link EventLoopGroup} can be used for clients that favors use of the "current" {@link EventLoop} for the
 * outbound connection. A current {@link EventLoop} is determined by checking if the thread calling {@link #next()} is
 * an eventloop instance belonging to this group. If so, the same instance is returned from {@link #next()} otherwise,
 * the call to {@link #next()} is delegated to the actual {@link EventLoopGroup} passed to this instance.
 *
 * This is generally useful for applications that process a recieved request by calling some other downstream
 * applications. If, during processing of these requests, there is no new thread introduced, then the {@link EventLoop}
 * processing the recieved request will also execute the outbound request to another application.
 *
 * The above, although being subtle has benefits around removing queuing while writing data to any channel in the entire
 * request processing.
 *
 * @author Nitesh Kant
 */
public class PreferCurrentEventLoopGroup implements EventLoopGroup {

    private final FastThreadLocal<EventLoop> self = new FastThreadLocal<>();
    private final EventLoopGroup delegate;

    public PreferCurrentEventLoopGroup(EventLoopGroup delegate) {
        this.delegate = delegate;
        Set<EventLoop> children = delegate.children();
        for (final EventLoop child : children) {
            child.submit(new Runnable() {
                @Override
                public void run() {
                    self.set(child);
                }
            }); // Since this is an optimization, there is no need for us to wait for this task to finish.
        }
    }

    @Override
    public EventLoop next() {
        final EventLoop thisEventLoop = self.get();
        return null != thisEventLoop ? thisEventLoop : delegate.next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    @Override
    public ChannelFuture register(Channel channel,
                                  ChannelPromise promise) {
        return next().register(channel, promise);
    }

    @Override
    public boolean isShuttingDown() {
        return delegate.isShuttingDown();
    }

    @Override
    public Future<?> shutdownGracefully() {
        return delegate.shutdownGracefully();
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout,
                                        TimeUnit unit) {
        return delegate.shutdownGracefully(quietPeriod, timeout, unit);
    }

    @Override
    public Future<?> terminationFuture() {
        return delegate.terminationFuture();
    }

    @Override
    @Deprecated
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    @Deprecated
    public Iterator<EventExecutor> iterator() {
        return delegate.iterator();
    }

    @Override
    public <E extends EventExecutor> Set<E> children() {
        return delegate.children();
    }

    @Override
    public Future<?> submit(Runnable task) {
        return next().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return next().submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return next().submit(task);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay,
                                       TimeUnit unit) {
        return next().schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit) {
        return next().schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                  long period,
                                                  TimeUnit unit) {
        return next().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay,
                                                     TimeUnit unit) {
        return next().scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return next().invokeAll(tasks);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
        return next().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return next().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return next().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        next().execute(command);
    }
}
