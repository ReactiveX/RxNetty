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

package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
import io.reactivex.netty.util.NoOpSubscriber;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * An observable connection for connection oriented protocols.
 *
 * @param <I> The type of the object that is read from this connection.
 * @param <O> The type of objects that are written to this connection.
 */
public class ObservableConnection<I, O> extends DefaultChannelWriter<O> {

    private Subject<I, I> inputSubject;
    @SuppressWarnings("rawtypes")private final MetricEventsSubject eventsSubject;
    private final ChannelMetricEventProvider metricEventProvider;
    /* Guarded by closeIssued so that its only updated once*/ protected volatile long closeStartTimeMillis = -1;

    protected ObservableConnection(final Channel channel, ChannelMetricEventProvider metricEventProvider,
                                   MetricEventsSubject<?> eventsSubject) {
        super(channel, eventsSubject, metricEventProvider);
        this.eventsSubject = eventsSubject;
        this.metricEventProvider = metricEventProvider;
        inputSubject = new SerializedSubject<I, I>(PublishSubject.<I>create());
    }

    public Observable<I> getInput() {
        return inputSubject;
    }

    public static <I, O>  ObservableConnection<I, O> create(final Channel channel,
                                                            final MetricEventsSubject<?> eventsSubject,
                                                            final ChannelMetricEventProvider metricEventProvider) {
        final ObservableConnection<I, O> toReturn = new ObservableConnection<I, O>(channel, metricEventProvider,
                                                                                eventsSubject);
        /**
         * Sending the event here does not leak "this" via the NewRxConnectionEvent as opposed to doing it inside the
         * constructor.
         */
        toReturn.fireNewRxConnectionEvent();

        return toReturn;
    }

    /**
     * Fires a {@link NewRxConnectionEvent} for this connection.
     *
     * <b>This must only be called before passing the connection instance to any other code.</b> The reason why this is
     * not done as part of the constructor is that {@link NewRxConnectionEvent} requires the {@link ObservableConnection}
     * instance which when sending from the constructor will escape "this"
     */
    protected void fireNewRxConnectionEvent() {
        ChannelHandlerContext firstContext = getChannel().pipeline().firstContext();
        firstContext.fireUserEventTriggered(new NewRxConnectionEvent(this, inputSubject));
    }

    /**
     * Closes this connection. This method is idempotent, so it can be called multiple times without any side-effect on
     * the channel. <br/>
     * This will also cancel any pending writes on the underlying channel. <br/>
     *
     * @return Observable signifying the close on the connection. Returns {@link rx.Observable#error(Throwable)} if the
     * close is already issued (may not be completed)
     */
    @Override
    public Observable<Void> close() {
        return super.close();
    }

    @Override
    protected Observable<Void> _close(boolean flush) {
        final Subject<I, I> thisSubject = inputSubject;
        ReadTimeoutPipelineConfigurator.disableReadTimeout(getChannel().pipeline());
        if (flush) {
            Observable<Void> toReturn = flush().lift(new Observable.Operator<Void, Void>() {
                @Override
                public Subscriber<? super Void> call(final Subscriber<? super Void> child) {
                    return new Subscriber<Void>() {
                        @Override
                        public void onCompleted() {
                            _closeChannel().subscribe(child);
                            thisSubject.onCompleted(); // Even though closeChannel() returns an Observable, close itself is eager.
                            // So this makes sure we send onCompleted() on subject after close is initialized.
                            // This results in more deterministic behavior for clients.
                        }

                        @Override
                        public void onError(Throwable e) {
                            child.onError(e);
                        }

                        @Override
                        public void onNext(Void aVoid) {
                            // Insignificant
                        }
                    };
                }
            });
            toReturn.subscribe(new NoOpSubscriber<Void>()); // Since subscribing to returned Observable is not required
                                                            // by the caller and we need to be subscribed to trigger the
                                                            // close of channel (_closeChannel()), it is required to
                                                            // subscribe to the returned Observable. We are not
                                                            // interested in the result so NoOpSub is used.
            return toReturn;
        } else {
            Observable<Void> toReturn = _closeChannel();
            thisSubject.onCompleted(); // Even though closeChannel() returns an Observable, close itself is eager.
            // So this makes sure we send onCompleted() on subject after close is initialized.
            // This results in more deterministic behavior for clients.
            return toReturn;
        }
    }

    @SuppressWarnings("unchecked")
    protected Observable<Void> _closeChannel() {
        closeStartTimeMillis = Clock.newStartTimeMillis();
        eventsSubject.onEvent(metricEventProvider.getChannelCloseStartEvent());

        final ChannelFuture closeFuture = getChannel().close();

        /**
         * This listener if added inside the returned Observable onSubscribe() function, would mean that the
         * metric events will only be fired if someone subscribed to the close() Observable. However, we need them to
         * fire independent of someone subscribing.
         */
        closeFuture.addListener(new ChannelFutureListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    eventsSubject.onEvent(metricEventProvider.getChannelCloseSuccessEvent(),
                                          Clock.onEndMillis(closeStartTimeMillis));
                } else {
                    eventsSubject.onEvent(metricEventProvider.getChannelCloseFailedEvent(),
                                          Clock.onEndMillis(closeStartTimeMillis), future.cause());
                }
            }
        });

        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                closeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
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

    protected void updateInputSubject(Subject<I, I> newSubject) {
        inputSubject = new SerializedSubject<I, I>(newSubject);
    }

}
