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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.FileRegion;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.functions.Func1;

/**
 * An implementation of {@link Connection} delegating all {@link ChannelOperations} methods to
 * {@link DefaultChannelOperations}.
 *
 * @author Nitesh Kant
 */
public final class ConnectionImpl<I, O> extends Connection<I, O> {

    private final ChannelOperations<O> delegate;

    public ConnectionImpl(Channel nettyChannel, MetricEventsSubject<?> eventsSubject,
                          ChannelMetricEventProvider metricEventProvider) {
        super(nettyChannel);
        delegate = new DefaultChannelOperations<>(nettyChannel, eventsSubject, metricEventProvider);
    }

    @Override
    public Observable<Void> write(O msg) {
        return delegate.write(msg);
    }

    @Override
    public Observable<Void> write(Observable<O> msgs) {
        return delegate.write(msgs);
    }

    @Override
    public Observable<Void> writeBytes(ByteBuf msg) {
        return delegate.writeBytes(msg);
    }

    @Override
    public Observable<Void> writeBytes(byte[] msg) {
        return delegate.writeBytes(msg);
    }

    @Override
    public Observable<Void> writeString(String msg) {
        return delegate.writeString(msg);
    }

    @Override
    public Observable<Void> writeFileRegion(FileRegion region) {
        return delegate.writeFileRegion(region);
    }

    @Override
    public Observable<Void> flush() {
        return delegate.flush();
    }

    @Override
    public Observable<Void> writeAndFlush(O msg) {
        return delegate.writeAndFlush(msg);
    }

    @Override
    public Observable<Void> writeAndFlush(Observable<O> msgs) {
        return delegate.writeAndFlush(msgs);
    }

    @Override
    public Observable<Void> writeAndFlush(Observable<O> msgs, Func1<O, Boolean> flushSelector) {
        return delegate.writeAndFlush(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeAndFlushOnEach(Observable<O> msgs) {
        return delegate.writeAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeBytesAndFlush(ByteBuf msg) {
        return delegate.writeBytesAndFlush(msg);
    }

    @Override
    public Observable<Void> writeBytesAndFlush(byte[] msg) {
        return delegate.writeBytesAndFlush(msg);
    }

    @Override
    public Observable<Void> writeStringAndFlush(String msg) {
        return delegate.writeStringAndFlush(msg);
    }

    @Override
    public Observable<Void> writeFileRegionAndFlush(FileRegion fileRegion) {
        return delegate.writeFileRegionAndFlush(fileRegion);
    }

    @Override
    public void cancelPendingWrites(boolean mayInterruptIfRunning) {
        delegate.cancelPendingWrites(mayInterruptIfRunning);
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return delegate.getAllocator();
    }

    @Override
    public Observable<Void> close() {
        return delegate.close();
    }

    @Override
    public Observable<Void> close(boolean flush) {
        return delegate.close(flush);
    }
}
