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
import io.netty.channel.ChannelFuture;
import io.netty.channel.FileRegion;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Nitesh Kant
 */
public class DefaultChannelWriter<O> implements ChannelWriter<O> {

    protected static final Observable<Void> CONNECTION_ALREADY_CLOSED =
            Observable.error(new IllegalStateException("Connection is already closed."));
    protected final AtomicBoolean closeIssued = new AtomicBoolean();

    private final Channel nettyChannel;
    private final FlushObservable flushObservable;

    protected DefaultChannelWriter(Channel nettyChannel, MetricEventsSubject<?> eventsSubject,
                                   ChannelMetricEventProvider metricEventProvider) {
        if (null == nettyChannel) {
            throw new NullPointerException("Channel can not be null.");
        }
        this.nettyChannel = nettyChannel;
        flushObservable = FlushObservable.create(eventsSubject, metricEventProvider, nettyChannel);
    }

    @Override
    public Observable<Void> writeAndFlush(O msg) {
        write(msg);
        return flush();
    }

    @Override
    public <R> Observable<Void> writeAndFlush(final R msg, final ContentTransformer<R> transformer) {
        write(msg, transformer);
        return flush();
    }

    @Override
    public Observable<Void> writeBytesAndFlush(ByteBuf msg) {
        writeBytes(msg);
        return flush();
    }

    @Override
    public void write(O msg) {
        writeOnChannel(msg);
    }

    @Override
    public <R> void write(R msg, ContentTransformer<R> transformer) {
        ByteBuf contentBytes = transformer.call(msg, getAllocator());
        writeOnChannel(contentBytes);
    }

    @Override
    public void writeBytes(ByteBuf msg) {
        write(msg, IdentityTransformer.DEFAULT_INSTANCE);
    }

    @Override
    public void writeBytes(byte[] msg) {
        write(msg, ByteTransformer.DEFAULT_INSTANCE);
    }

    @Override
    public void writeString(String msg) {
        write(msg, new StringTransformer());
    }

    @Override
    public Observable<Void> writeBytesAndFlush(byte[] msg) {
        write(msg, ByteTransformer.DEFAULT_INSTANCE);
        return flush();
    }

    @Override
    public Observable<Void> writeStringAndFlush(String msg) {
        write(msg, new StringTransformer());
        return flush();
    }
    
    @Override
    public void writeFileRegion(FileRegion region) {
        writeOnChannel(region);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Observable<Void> flush() {
        return flushObservable;
    }

    @Override
    public void cancelPendingWrites(boolean mayInterruptIfRunning) {
        flushObservable.cancelPendingFutures(mayInterruptIfRunning);
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return nettyChannel.alloc();
    }

    protected ChannelFuture writeOnChannel(Object msg) {
        ChannelFuture writeFuture = getChannel().write(msg); // Calling write on context will be wrong as the context will be of a component not necessarily, the tail of the pipeline.
        flushObservable.add(writeFuture);
        return writeFuture;
    }

    public Channel getChannel() {
        return nettyChannel;
    }

    public boolean isCloseIssued() {
        return closeIssued.get();
    }

    @Override
    public Observable<Void> close() {
        return close(false);
    }

    @Override
    public Observable<Void> close(boolean flush) {
        if (closeIssued.compareAndSet(false, true)) {
            return _close(flush);
        } else {
            return CONNECTION_ALREADY_CLOSED;
        }
    }

    protected Observable<Void> _close(boolean flush) {
        return Observable.empty();
    }

}
