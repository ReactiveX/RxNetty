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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.FileRegion;
import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.util.concurrent.TimeUnit.*;

/**
 * Default implementation for {@link ChannelOperations}.
 *
 * @param <W> Type of data that can be written on the associated channel.
 */
public class DefaultChannelOperations<W> implements ChannelOperations<W> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelOperations.class);

    /** Field updater for closeIssued. */
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<DefaultChannelOperations> CLOSE_ISSUED_UPDATER
            = AtomicIntegerFieldUpdater.newUpdater(DefaultChannelOperations.class, "closeIssued");
    @SuppressWarnings("unused")
    private volatile int closeIssued; // updated by the atomic updater, so required to be volatile.

    private final Channel nettyChannel;
    private final ConnectionEventListener eventListener;
    private final EventPublisher eventPublisher;

    private final Observable<Void> closeObservable;
    private final Observable<Void> flushAndCloseObservable;

    private final Func1<W, Boolean> flushOnEachSelector = new Func1<W, Boolean>() {
        @Override
        public Boolean call(W w) {
            return true;
        }
    };

    public DefaultChannelOperations(final Channel nettyChannel, ConnectionEventListener eventListener,
                                    EventPublisher eventPublisher) {
        this.nettyChannel = nettyChannel;
        this.eventListener = eventListener;
        this.eventPublisher = eventPublisher;
        closeObservable = Observable.create(new OnSubscribeForClose(nettyChannel));
        flushAndCloseObservable = closeObservable.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                flush();
            }
        });
    }

    @Override
    public Observable<Void> write(final Observable<W> msgs) {
        return _write(msgs);
    }

    @Override
    public Observable<Void> write(Observable<W> msgs, final Func1<W, Boolean> flushSelector) {
        return _write(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeAndFlushOnEach(Observable<W> msgs) {
        return _write(msgs, flushOnEachSelector);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs) {
        return _write(msgs);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector) {
        return _write(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeStringAndFlushOnEach(Observable<String> msgs) {
        return writeString(msgs, FLUSH_ON_EACH_STRING);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs) {
        return _write(msgs);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector) {
        return _write(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
        return _write(msgs, FLUSH_ON_EACH_BYTES);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs) {
        return _write(msgs);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs, Func1<FileRegion, Boolean> flushSelector) {
        return _write(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeFileRegionAndFlushOnEach(Observable<FileRegion> msgs) {
        return writeFileRegion(msgs, FLUSH_ON_EACH_FILE_REGION);
    }

    @Override
    public <WW> ChannelOperations<WW> transformWrite(AllocatingTransformer<WW, W> transformer) {
        nettyChannel.pipeline().fireUserEventTriggered(new AppendTransformerEvent<>(transformer));
        return new DefaultChannelOperations<>(nettyChannel, eventListener, eventPublisher);
    }

    @Override
    public void flush() {
        if (eventPublisher.publishingEnabled()) {
            final long startTimeNanos = Clock.newStartTimeNanos();
            eventListener.onFlushStart();
            if (nettyChannel.eventLoop().inEventLoop()) {
                _flushInEventloop(startTimeNanos);
            } else {
                nettyChannel.eventLoop()
                            .execute(new Runnable() {
                                @Override
                                public void run() {
                                    _flushInEventloop(startTimeNanos);
                                }
                            });
            }
        } else {
            nettyChannel.flush();
        }
    }

    @Override
    public Observable<Void> close() {
        return close(true);
    }

    @Override
    public Observable<Void> close(boolean flush) {
        return flush ? flushAndCloseObservable : closeObservable;
    }

    @Override
    public void closeNow() {
        close().subscribe(Actions.empty(), new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.error("Error closing connection.", throwable);
            }
        });
    }

    @Override
    public Observable<Void> closeListener() {
        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                final SubscriberToChannelFutureBridge l = new SubscriberToChannelFutureBridge() {

                    @Override
                    protected void doOnSuccess(ChannelFuture future) {
                        subscriber.onCompleted();
                    }

                    @Override
                    protected void doOnFailure(ChannelFuture future, Throwable cause) {
                        subscriber.onCompleted();
                    }
                };
                l.bridge(nettyChannel.closeFuture(), subscriber);
            }
        });
    }

    private <X> Observable<Void> _write(final Observable<X> msgs, Func1<X, Boolean> flushSelector) {
        return _write(msgs.lift(new FlushSelectorOperator<>(flushSelector, this)));
    }

    private void _flushInEventloop(long startTimeNanos) {
        assert nettyChannel.eventLoop().inEventLoop();
        nettyChannel.flush(); // Flush is sync when from eventloop.
        eventListener.onFlushComplete(Clock.onEndNanos(startTimeNanos), NANOSECONDS);
    }

    private Observable<Void> _write(final Observable<?> msgs) {
        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {

                final long startTimeNanos = Clock.newStartTimeNanos();

                if (eventPublisher.publishingEnabled()) {
                    eventListener.onWriteStart();
                }

                /*
                 * If a write happens from outside the eventloop, it does not wakeup the selector, till a flush happens.
                 * In absence of a selector wakeup, this write will be delayed by the selector sleep interval.
                 * The code below makes sure that the selector is woken up on a write (by executing a task that does
                 * the write)
                 */
                if (nettyChannel.eventLoop().inEventLoop()) {
                    _writeStreamToChannel(subscriber, startTimeNanos);
                } else {
                    nettyChannel.eventLoop()
                                .execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        _writeStreamToChannel(subscriber, startTimeNanos);
                                    }
                                });
                }
            }

            private void _writeStreamToChannel(final Subscriber<? super Void> subscriber, final long startTimeNanos) {
                final ChannelFuture writeFuture = nettyChannel.write(msgs.doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Boolean shdNotFlush = nettyChannel.attr(FLUSH_ONLY_ON_READ_COMPLETE).get();
                        if (null == shdNotFlush || !shdNotFlush) {
                            flush();
                        }
                    }
                }));
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        writeFuture.cancel(false); // cancel write on unsubscribe.
                    }
                }));
                writeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (subscriber.isUnsubscribed()) {
                            /*short-circuit if subscriber is unsubscribed*/
                            return;
                        }

                        if (future.isSuccess()) {
                            if (eventPublisher.publishingEnabled()) {
                                eventListener.onWriteSuccess(Clock.onEndNanos(startTimeNanos), NANOSECONDS);
                            }
                            subscriber.onCompleted();
                        } else {
                            if (eventPublisher.publishingEnabled()) {
                                eventListener.onWriteFailed(Clock.onEndNanos(startTimeNanos), NANOSECONDS,
                                                            future.cause());
                            }
                            subscriber.onError(future.cause());
                        }
                    }
                });
            }
        });
    }

    private class OnSubscribeForClose implements OnSubscribe<Void> {

        private final Channel nettyChannel;

        public OnSubscribeForClose(Channel nettyChannel) {
            this.nettyChannel = nettyChannel;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void call(final Subscriber<? super Void> subscriber) {

            final long closeStartTimeNanos = Clock.newStartTimeNanos();

            final ChannelCloseListener closeListener;
            if (CLOSE_ISSUED_UPDATER.compareAndSet(DefaultChannelOperations.this, 0, 1)) {
                if (eventPublisher.publishingEnabled()) {
                    eventListener.onConnectionCloseStart();
                }

                nettyChannel.close(); // close only once.

                closeListener = new ChannelCloseListener(eventListener, eventPublisher, closeStartTimeNanos,
                                                         subscriber);
            } else {
                closeListener = new ChannelCloseListener(subscriber);
            }

            closeListener.bridge(nettyChannel.closeFuture(), subscriber);
        }

        private class ChannelCloseListener extends SubscriberToChannelFutureBridge {

            private final long closeStartTimeNanos;
            private final Subscriber<? super Void> subscriber;
            private final ConnectionEventListener eventListener;
            private final EventPublisher eventPublisher;

            public ChannelCloseListener(ConnectionEventListener eventListener, EventPublisher eventPublisher,
                                        long closeStartTimeNanos, Subscriber<? super Void> subscriber) {
                this.eventListener = eventListener;
                this.eventPublisher = eventPublisher;
                this.closeStartTimeNanos = closeStartTimeNanos;
                this.subscriber = subscriber;
            }

            public ChannelCloseListener(Subscriber<? super Void> subscriber) {
                this(null, null, -1, subscriber);
            }

            @Override
            protected void doOnSuccess(ChannelFuture future) {
                if (null != eventListener && eventPublisher.publishingEnabled()) {
                    eventListener.onConnectionCloseSuccess(Clock.onEndNanos(closeStartTimeNanos), NANOSECONDS);
                }
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
            }

            @Override
            protected void doOnFailure(ChannelFuture future, Throwable cause) {
                if (null != eventListener && eventPublisher.publishingEnabled()) {
                    eventListener.onConnectionCloseFailed(Clock.onEndNanos(closeStartTimeNanos), NANOSECONDS,
                                                          future.cause());
                }
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(future.cause());
                }
            }
        }
    }
}
