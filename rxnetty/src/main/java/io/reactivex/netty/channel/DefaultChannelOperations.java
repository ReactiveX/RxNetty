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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.FileRegion;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.Observable.OnSubscribe;
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

        flushAndCloseObservable = closeObservable.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                nettyChannel.flush();
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
    public void flush() {
        nettyChannel.flush();
    }

    @Override
    public Observable<Void> close() {
        return close(true);
    }

    @Override
    public Observable<Void> close(boolean flush) {
        return flush ? flushAndCloseObservable : closeObservable;
    }

    private <X> Observable<Void> _write(final Observable<X> msgs, Func1<X, Boolean> flushSelector) {
        return _write(msgs.lift(new FlushSelectorOperator<X>(flushSelector, nettyChannel)));
    }

    private Observable<Void> _write(final Observable<?> msgs) {
        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                final ChannelFuture writeFuture = nettyChannel.write(msgs);
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
