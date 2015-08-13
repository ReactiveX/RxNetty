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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.FileRegion;
import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.events.EventPublisher;
import rx.Observable;
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

    public static <R, W> ConnectionImpl<R, W> create(Channel nettyChannel, ConnectionEventListener eventListener,
                                                     EventPublisher eventPublisher) {
        final ConnectionImpl<R, W> toReturn = new ConnectionImpl<>(nettyChannel, eventListener, eventPublisher);
        toReturn.connectCloseToChannelClose();
        return toReturn;
    }

    /*Visible for testing*/static <R, W> ConnectionImpl<R, W> create(Channel nettyChannel,
                                                                     ChannelOperations<W> delegate) {
        final ConnectionImpl<R, W> toReturn = new ConnectionImpl<>(nettyChannel, delegate);
        toReturn.connectCloseToChannelClose();
        return toReturn;
    }
}
