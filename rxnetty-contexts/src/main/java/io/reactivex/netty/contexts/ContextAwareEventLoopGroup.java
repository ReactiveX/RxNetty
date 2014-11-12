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
package io.reactivex.netty.contexts;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Nitesh Kant
 */
public class ContextAwareEventLoopGroup implements EventLoopGroup {

    private final EventLoopGroup delegate;
    private final ContextCapturer capturer;

    public ContextAwareEventLoopGroup(EventLoopGroup delegate, ContextCapturer capturer) {
        this.delegate = delegate;
        this.capturer = capturer;
    }

    @Override
    public EventLoop next() {
        return delegate.next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return delegate.register(channel);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return delegate.register(channel, promise);
    }

    @Override
    public boolean isShuttingDown() {
        return delegate.isShuttingDown();
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return delegate.iterator();
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
                                           TimeUnit unit) {
        return delegate.schedule(capturer.makeClosure(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(capturer.makeClosure(command), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                                                  TimeUnit unit) {
        return delegate.scheduleAtFixedRate(capturer.makeClosure(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                                                     TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(capturer.makeClosure(command), initialDelay, delay, unit);
    }

    @Override
    @Deprecated
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public Future<?> shutdownGracefully() {
        return delegate.shutdownGracefully();
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return delegate.shutdownGracefully(quietPeriod, timeout, unit);
    }

    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(capturer.makeClosure(task));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(capturer.makeClosure(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(capturer.makeClosure(task), result);
    }

    @Override
    public Future<?> terminationFuture() {
        return delegate.terminationFuture();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        final Collection<Callable<T>> decoratedTasks = decorateCallables(tasks);
        return delegate.invokeAll(decoratedTasks);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                                              long timeout, TimeUnit unit) throws InterruptedException {
        final Collection<Callable<T>> decoratedTasks = decorateCallables(tasks);
        return delegate.invokeAll(decoratedTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Collection<Callable<T>> decoratedTasks = decorateCallables(tasks);
        return delegate.invokeAny(decoratedTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Collection<Callable<T>> decoratedTasks = decorateCallables(tasks);
        return delegate.invokeAny(decoratedTasks, timeout, unit);
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
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    private <T> Collection<Callable<T>> decorateCallables(Collection<? extends Callable<T>> tasks) {
        final Collection<Callable<T>> allTasks = new ArrayList<Callable<T>>(tasks.size());
        for (Iterator<? extends Callable<T>> iterator = tasks.iterator(); iterator.hasNext(); ) {
            Callable<T> task = iterator.next();
            allTasks.add(capturer.makeClosure(task));
            iterator.remove();
        }
        return allTasks;
    }

}
