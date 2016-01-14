/*
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FileRegion;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.events.EventAttributeKeys;
import io.reactivex.netty.events.EventPublisher;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * An implementation of {@link Connection} delegating all {@link ChannelOperations} methods to
 * {@link DefaultChannelOperations}.
 */
public final class ConnectionImpl<R, W> extends Connection<R, W> {

    private final ChannelOperations<W> delegate;

    private ConnectionImpl(Channel nettyChannel, ConnectionEventListener eventListener, EventPublisher eventPublisher) {
        super(nettyChannel);
        delegate = new DefaultChannelOperations<>(nettyChannel, eventListener, eventPublisher);
    }

    private ConnectionImpl(Channel nettyChannel, ChannelOperations<W> delegate) {
        super(nettyChannel);
        this.delegate = delegate;
    }

    @Override
    public Observable<Void> write(Observable<W> msgs) {
        return delegate.write(msgs);
    }

    @Override
    public Observable<Void> write(Observable<W> msgs, Func1<W, Boolean> flushSelector) {
        return delegate.write(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeAndFlushOnEach(Observable<W> msgs) {
        return delegate.writeAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs) {
        return delegate.writeString(msgs);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector) {
        return delegate.writeString(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeStringAndFlushOnEach(Observable<String> msgs) {
        return delegate.writeStringAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs) {
        return delegate.writeBytes(msgs);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs,
                                       Func1<byte[], Boolean> flushSelector) {
        return delegate.writeBytes(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
        return delegate.writeBytesAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs) {
        return delegate.writeFileRegion(msgs);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs,
                                            Func1<FileRegion, Boolean> flushSelector) {
        return delegate.writeFileRegion(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeFileRegionAndFlushOnEach(Observable<FileRegion> msgs) {
        return delegate.writeFileRegionAndFlushOnEach(msgs);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public Observable<Void> close() {
        return delegate.close();
    }

    @Override
    public Observable<Void> close(boolean flush) {
        return delegate.close(flush);
    }

    @Override
    public void closeNow() {
        delegate.closeNow();
    }

    @Override
    public Observable<Void> closeListener() {
        return delegate.closeListener();
    }

    public static <R, W> ConnectionImpl<R, W> fromChannel(Channel nettyChannel) {
        EventPublisher ep = nettyChannel.attr(EventAttributeKeys.EVENT_PUBLISHER).get();
        if (null == ep) {
            throw new IllegalArgumentException("No event publisher set in the channel.");
        }

        ConnectionEventListener l = null;
        if (ep.publishingEnabled()) {
            l = nettyChannel.attr(EventAttributeKeys.CONNECTION_EVENT_LISTENER).get();
            if (null == l) {
                throw new IllegalArgumentException("No event listener set in the channel.");
            }
        }

        final ConnectionImpl<R, W> toReturn = new ConnectionImpl<>(nettyChannel, l, ep);
        toReturn.connectCloseToChannelClose();
        return toReturn;
    }

    /*Visible for testing*/static <R, W> ConnectionImpl<R, W> create(Channel nettyChannel,
                                                                     ChannelOperations<W> delegate) {
        final ConnectionImpl<R, W> toReturn = new ConnectionImpl<>(nettyChannel, delegate);
        toReturn.connectCloseToChannelClose();
        return toReturn;
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerFirst(String name, ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addFirst(name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                              ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addFirst(group, name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerLast(String name, ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addLast(name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                             ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addLast(group, name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerBefore(String baseName, String name, ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addBefore(baseName, name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                               ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addBefore(group, baseName, name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerAfter(String baseName, String name, ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addAfter(baseName, name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                              ChannelHandler handler) {
        getResettableChannelPipeline().markIfNotYetMarked().addAfter(group, baseName, name, handler);
        return cast();
    }

    @Override
    public <RR, WW> Connection<RR, WW> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        pipelineConfigurator.call(getResettableChannelPipeline().markIfNotYetMarked());
        return cast();
    }

    @SuppressWarnings("unchecked")
    protected  <RR, WW> Connection<RR, WW> cast() {
        return (Connection<RR, WW>) this;
    }
}
