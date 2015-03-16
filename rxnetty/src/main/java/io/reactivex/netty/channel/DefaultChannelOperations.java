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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.FileRegion;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Default implementation for {@link ChannelOperations}.
 *
 * @param <W> Type of data that can be written on the associated channel.
 *
 * @author Nitesh Kant
 */
public class DefaultChannelOperations<W> implements ChannelOperations<W> {

    /** Field updater for closeIssued. */
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<DefaultChannelOperations> CLOSE_ISSUED_UPDATER
            = AtomicIntegerFieldUpdater.newUpdater(DefaultChannelOperations.class, "closeIssued");
    @SuppressWarnings("unused")
    private volatile int closeIssued; // updated by the atomic updater, so required to be volatile.

    private final Channel nettyChannel;

    @SuppressWarnings("rawtypes")
    private final MetricEventsSubject eventsSubject;
    private final Observable<Void> closeObservable;
    private final FlushObservable flushObservable;
    private final Observable<Void> flushWithWriteObservable;
    private final Observable<Void> flushAndCloseObservable;
    private final Func1<W, Boolean> flushOnEachSelector = new Func1<W, Boolean>() {
        @Override
        public Boolean call(W w) {
            return true;
        }
    };


    public DefaultChannelOperations(final Channel nettyChannel, final MetricEventsSubject<?> eventsSubject,
                                    final ChannelMetricEventProvider metricEventProvider) {
        this.nettyChannel = nettyChannel;
        this.eventsSubject = eventsSubject;

        closeObservable = Observable.create(new OnSubscribeForClose(metricEventProvider, nettyChannel));

        flushObservable = FlushObservable.create(eventsSubject, metricEventProvider, nettyChannel);

        /**
         * Errors get connected to the write only as the _writeAndFlush() semantics are to give error only for the write
         * done along with the write*AndFlush() call.
         */
        flushWithWriteObservable = flushObservable.onErrorResumeNext(Observable.<Void>empty());

        flushAndCloseObservable = flushObservable
                // Since concat does not subscribe to the other Observable on error, this makes sure that the close is
                // closed in the case of error too.
                .onErrorResumeNext(new Func1<Throwable, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Throwable throwable) {
                        // Since, flush has failed, the result should always be an error but close must be invoked.
                        return closeObservable.concatWith(Observable.<Void>error(throwable));
                    }
                })
                .concatWith(closeObservable); // For success paths.
    }

    @Override
    public Observable<Void> write(W msg) {
        return _write(msg);
    }

    @Override
    public Observable<Void> write(final Observable<W> msgs) {
        return _write(msgs);
    }

    @Override
    public Observable<Void> writeBytes(ByteBuf msg) {
        return _write(msg);
    }

    @Override
    public Observable<Void> writeBytes(byte[] msg) {
        return _write(msg);
    }

    @Override
    public Observable<Void> writeString(String msg) {
        return _write(msg);
    }

    @Override
    public Observable<Void> writeFileRegion(FileRegion region) {
        return _write(region);
    }

    @Override
    public Observable<Void> flush() {
        return flushObservable;
    }

    @Override
    public Observable<Void> writeAndFlush(W msg) {
        return _writeAndFlush(msg);
    }

    @Override
    public Observable<Void> writeAndFlush(final Observable<W> msgs) {
        return _writeAndFlushStream(msgs);
    }

    @Override
    public Observable<Void> writeAndFlush(Observable<W> msgs, final Func1<W, Boolean> flushSelector) {

        /**
         * This is trading correctness of the flush of pending messages just when the first time flushSelector returns
         * true with simplicity of just being able to do a channel.flush() instead of flatmap of FlushObservable on
         * every flush.
         */
        return flushObservable.concatWith(_write(msgs.lift(new Operator<W, W>() {
            @Override
            public Subscriber<? super W> call(final Subscriber<? super W> subscriber) {
                return new Subscriber<W>(subscriber) {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(W w) {
                        subscriber.onNext(w);
                        if (flushSelector.call(w)) {
                            /**
                             * Since, the return is only going to be the result of all writes, there is no need to track
                             * the result of each flush here. Hence, this just does a channel flush instead of using a
                             * FlushObservable.
                             */
                            nettyChannel.flush();
                        }
                    }
                };
            }
        })));
    }

    @Override
    public Observable<Void> writeAndFlushOnEach(Observable<W> msgs) {
        return writeAndFlush(msgs, flushOnEachSelector);
    }

    @Override
    public Observable<Void> writeBytesAndFlush(ByteBuf msg) {
        return _writeAndFlush(msg);
    }

    @Override
    public Observable<Void> writeBytesAndFlush(byte[] msg) {
        return _writeAndFlush(msg);
    }

    @Override
    public Observable<Void> writeStringAndFlush(String msg) {
        return _writeAndFlush(msg);
    }

    @Override
    public Observable<Void> writeFileRegionAndFlush(FileRegion fileRegion) {
        return _writeAndFlush(fileRegion);
    }

    @Override
    public void cancelPendingWrites(boolean mayInterruptIfRunning) {
        flushObservable.cancelPendingFutures(mayInterruptIfRunning);
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return nettyChannel.alloc();
    }

    @Override
    public Observable<Void> close() {
        return close(true);
    }

    @Override
    public Observable<Void> close(boolean flush) {
        return flushAndCloseObservable;
    }

    private Observable<Void> _write(final Object msg) {
        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                final ChannelFuture writeFuture = nettyChannel.write(msg);
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
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(future.cause());
                        }
                    }
                });
                flushObservable.add(writeFuture);
            }
        });
    }

    private Observable<Void> _writeAndFlush(final Object msg) {
        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                flushObservable.add(nettyChannel.write(msg)
                                                .addListener(new ChannelFutureListener() {
                                                    @Override
                                                    public void operationComplete(ChannelFuture future)
                                                            throws Exception {
                                                        /**
                                                         * The semantics here are that the error on this write
                                                         * must only propagate to the subscriber.
                                                         * This is why, the subscriber is not directly
                                                         * subscribed to the flushObservable.
                                                         * This code is propagating this write failure and
                                                         * flushWithWriteObservable propagates completion.
                                                         */
                                                        if (!future.isSuccess()) {
                                                            subscriber.onError(future.cause());
                                                        }
                                                    }
                                                }));

                /*
                 * Flushes and connects completion to the subscriber. Error for this write is propagated via the
                 * channel listener above.
                 */
                flushWithWriteObservable.subscribe(subscriber);
            }
        });
    }

    private Observable<Void> _writeAndFlushStream(final Observable<W> msg) {
        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                Observable<W> msgStream = msg.doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        /*
                         * Flushes and connects completion to the subscriber. Error for this write is propagated via the
                         * channel listener below.
                         */
                        flushWithWriteObservable.subscribe(subscriber);
                    }
                });

                flushObservable.add(nettyChannel.write(msgStream)
                                                .addListener(new ChannelFutureListener() {
                                                    @Override
                                                    public void operationComplete(ChannelFuture future)
                                                            throws Exception {
                                                        /**
                                                         * The semantics here are that the error on this write
                                                         * must only propagate to the subscriber.
                                                         * This is why, the subscriber is not directly
                                                         * subscribed to the flushObservable.
                                                         * This code is propagating this write failure and
                                                         * flushWithWriteObservable propagates completion.
                                                         */
                                                        if (!future.isSuccess()) {
                                                            subscriber.onError(future.cause());
                                                        }
                                                    }
                                                }));
            }
        });
    }

    private class OnSubscribeForClose implements OnSubscribe<Void> {

        private final ChannelMetricEventProvider metricEventProvider;
        private final Channel nettyChannel;

        public OnSubscribeForClose(ChannelMetricEventProvider metricEventProvider, Channel nettyChannel) {
            this.metricEventProvider = metricEventProvider;
            this.nettyChannel = nettyChannel;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void call(final Subscriber<? super Void> subscriber) {

            final long closeStartTimeMillis = Clock.newStartTimeMillis();

            ChannelCloseListener closeListener;
            if (CLOSE_ISSUED_UPDATER.compareAndSet(DefaultChannelOperations.this, 0, 1)) {
                // TODO: Disable read timeout handler.

                eventsSubject.onEvent(metricEventProvider.getChannelCloseStartEvent());

                nettyChannel.close(); // close only once.

                closeListener = new ChannelCloseListener(eventsSubject, closeStartTimeMillis, subscriber);
            } else {
                closeListener = new ChannelCloseListener(subscriber);
            }

            nettyChannel.closeFuture().addListener(closeListener);

        }

        private class ChannelCloseListener implements ChannelFutureListener {

            private final long closeStartTimeMillis;
            private final Subscriber<? super Void> subscriber;
            @SuppressWarnings("rawtypes")
            private final MetricEventsSubject eventsSubjectNullIfNoEventPub;

            public ChannelCloseListener(MetricEventsSubject<?> eventsSubject, long closeStartTimeMillis,
                                        Subscriber<? super Void> subscriber) {
                eventsSubjectNullIfNoEventPub = eventsSubject;
                this.closeStartTimeMillis = closeStartTimeMillis;
                this.subscriber = subscriber;
            }

            public ChannelCloseListener(Subscriber<? super Void> subscriber) {
                this(null, -1, subscriber);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    if (null != eventsSubjectNullIfNoEventPub) {
                        eventsSubjectNullIfNoEventPub.onEvent(metricEventProvider.getChannelCloseSuccessEvent(),
                                                              Clock.onEndMillis(closeStartTimeMillis));
                    }
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                } else {
                    if (null != eventsSubjectNullIfNoEventPub) {
                        eventsSubjectNullIfNoEventPub.onEvent(metricEventProvider.getChannelCloseFailedEvent(),
                                                              Clock.onEndMillis(closeStartTimeMillis), future.cause());
                    }
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(future.cause());
                    }
                }
            }
        }
    }
}
