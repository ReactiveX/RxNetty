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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An implementation of {@link ChannelPipeline} that allows a mark-reset scheme for {@link ChannelHandler}s. This allows
 * temporary modifications to the underlying {@link ChannelPipeline} instance for usecases like pooled connections,
 * server response upgrades, etc.
 *
 * <b>This only supports a single mark at a time, although mark-reset-mark cycles can be repeated any number of times.</b>
 *
 * <h2>Usage:</h2>
 *
 * To start recording resetable changes, call {@link #mark()} and to reset back to the state before {@link #mark()} was
 * called, call {@link #reset()}
 *
 * <h2>Thread safety</h2>
 *
 * All operations of {@link ChannelPipeline} are delegated to the passed {@link ChannelPipeline} instance while
 * creation. {@link #mark()} and {@link #reset()} uses the same mutex as {@link ChannelPipeline} for synchronization
 * across different method calls.
 */
public final class MarkAwarePipeline implements ChannelPipeline {

    private boolean marked; // Guarded by this

    private final ChannelPipeline delegate;

    public MarkAwarePipeline(ChannelPipeline delegate) {
        this.delegate = delegate;
    }

    /**
     * Marks this pipeline and record further changes which can be reverted by calling {@link #reset()}
     *
     * @throws IllegalStateException If this method is called more than once without calling {@link #reset()} in
     * between.
     */
    public synchronized MarkAwarePipeline mark() {
        if (marked) {
            throw new IllegalStateException("Pipeline does not support nested marks.");
        }
        return this;
    }

    /**
     * Marks this pipeline and record further changes which can be reverted by calling {@link #reset()}
     *
     * @throws IllegalStateException If this method is called more than once without calling {@link #reset()} in
     * between.
     */
    public synchronized MarkAwarePipeline markIfNotYetMarked() {
        if (!marked) {
            return mark();
        }
        return this;
    }

    /**
     * If {@link #mark()} was called before, resets the pipeline to the state it was before calling {@link #mark()}.
     * Otherwise, ignores the reset.
     */
    public synchronized MarkAwarePipeline reset() {
        if (!marked) {
            return this; /*If there is no mark, there is nothing to reset.*/
        }

        marked = false;

        return this;
    }

    public synchronized boolean isMarked() {
        return marked;
    }

    @Override
    public ChannelPipeline addFirst(String name, ChannelHandler handler) {
        delegate.addFirst(name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group,
                                    String name, ChannelHandler handler) {
        delegate.addFirst(group, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandlerInvoker invoker,
                                    String name, ChannelHandler handler) {
        delegate.addFirst(invoker, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addLast(String name, ChannelHandler handler) {
        delegate.addLast(name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group,
                                   String name, ChannelHandler handler) {
        delegate.addLast(group, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandlerInvoker invoker,
                                   String name, ChannelHandler handler) {
        delegate.addLast(invoker, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addBefore(String baseName, String name,
                                     ChannelHandler handler) {
        return delegate.addBefore(baseName, name, handler);
    }

    @Override
    public ChannelPipeline addBefore(EventExecutorGroup group,
                                     String baseName, String name,
                                     ChannelHandler handler) {
        delegate.addBefore(group, baseName, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addBefore(ChannelHandlerInvoker invoker,
                                     String baseName, String name,
                                     ChannelHandler handler) {
        delegate.addBefore(invoker, baseName, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(String baseName, String name,
                                    ChannelHandler handler) {
        delegate.addAfter(baseName, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(EventExecutorGroup group,
                                    String baseName, String name,
                                    ChannelHandler handler) {
        delegate.addAfter(group, baseName, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(ChannelHandlerInvoker invoker,
                                    String baseName, String name,
                                    ChannelHandler handler) {
        delegate.addAfter(invoker, baseName, name, handler);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandler... handlers) {
        delegate.addFirst(handlers);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group,
                                    ChannelHandler... handlers) {
        delegate.addFirst(group, handlers);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandlerInvoker invoker,
                                    ChannelHandler... handlers) {
        delegate.addFirst(invoker, handlers);
        return this;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandler... handlers) {
        delegate.addLast(handlers);
        return this;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group,
                                   ChannelHandler... handlers) {
        delegate.addLast(group, handlers);
        return this;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandlerInvoker invoker,
                                   ChannelHandler... handlers) {
        delegate.addLast(invoker, handlers);
        return this;
    }

    @Override
    public ChannelPipeline remove(ChannelHandler handler) {
        delegate.remove(handler);
        return this;
    }

    @Override
    public ChannelHandler remove(String name) {
        return delegate.remove(name);
    }

    @Override
    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return delegate.remove(handlerType);
    }

    @Override
    public ChannelHandler removeFirst() {
        return delegate.removeFirst();
    }

    @Override
    public ChannelHandler removeLast() {
        return delegate.removeLast();
    }

    @Override
    public ChannelPipeline replace(ChannelHandler oldHandler,
                                   String newName, ChannelHandler newHandler) {
        delegate.replace(oldHandler, newName, newHandler);
        return this;
    }

    @Override
    public ChannelHandler replace(String oldName, String newName,
                                  ChannelHandler newHandler) {
        return delegate.replace(oldName, newName, newHandler);
    }

    @Override
    public <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName,
                                                ChannelHandler newHandler) {
        return delegate.replace(oldHandlerType, newName, newHandler);
    }

    @Override
    public ChannelHandler first() {
        return delegate.first();
    }

    @Override
    public ChannelHandlerContext firstContext() {
        return delegate.firstContext();
    }

    @Override
    public ChannelHandler last() {
        return delegate.last();
    }

    @Override
    public ChannelHandlerContext lastContext() {
        return delegate.lastContext();
    }

    @Override
    public ChannelHandler get(String name) {
        return delegate.get(name);
    }

    @Override
    public <T extends ChannelHandler> T get(Class<T> handlerType) {
        return delegate.get(handlerType);
    }

    @Override
    public ChannelHandlerContext context(ChannelHandler handler) {
        return delegate.context(handler);
    }

    @Override
    public ChannelHandlerContext context(String name) {
        return delegate.context(name);
    }

    @Override
    public ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
        return delegate.context(handlerType);
    }

    @Override
    public Channel channel() {
        return delegate.channel();
    }

    @Override
    public List<String> names() {
        return delegate.names();
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        return delegate.toMap();
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        delegate.fireChannelRegistered();
        return this;
    }

    @Override
    public ChannelPipeline fireChannelUnregistered() {
        delegate.fireChannelUnregistered();
        return this;
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        delegate.fireChannelActive();
        return this;
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        delegate.fireChannelInactive();
        return this;
    }

    @Override
    public ChannelPipeline fireExceptionCaught(Throwable cause) {
        delegate.fireExceptionCaught(cause);
        return this;
    }

    @Override
    public ChannelPipeline fireUserEventTriggered(Object event) {
        delegate.fireUserEventTriggered(event);
        return this;
    }

    @Override
    public ChannelPipeline fireChannelRead(Object msg) {
        delegate.fireChannelRead(msg);
        return this;
    }

    @Override
    public ChannelPipeline fireChannelReadComplete() {
        delegate.fireChannelReadComplete();
        return this;
    }

    @Override
    public ChannelPipeline fireChannelWritabilityChanged() {
        delegate.fireChannelWritabilityChanged();
        return this;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return delegate.bind(localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return delegate.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress,
                                 SocketAddress localAddress) {
        return delegate.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture disconnect() {
        return delegate.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return delegate.close();
    }

    @Override
    public ChannelFuture deregister() {
        return delegate.deregister();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress,
                              ChannelPromise promise) {
        return delegate.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress,
                                 ChannelPromise promise) {
        return delegate.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress,
                                 SocketAddress localAddress,
                                 ChannelPromise promise) {
        return delegate.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return delegate.disconnect(promise);
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return delegate.close(promise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return delegate.deregister(promise);
    }

    @Override
    public ChannelPipeline read() {
        return delegate.read();
    }

    @Override
    public ChannelFuture write(Object msg) {
        return delegate.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return delegate.write(msg, promise);
    }

    @Override
    public ChannelPipeline flush() {
        return delegate.flush();
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return delegate.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return delegate.writeAndFlush(msg);
    }

    @Override
    public Iterator<Entry<String, ChannelHandler>> iterator() {
        return delegate.iterator();
    }
}
