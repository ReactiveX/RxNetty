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
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import rx.Producer;
import rx.Subscriber;
import rx.annotations.Beta;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.internal.operators.NotificationLite;
import rx.subscriptions.Subscriptions;

/**
 * A subscriber for {@link Connection} input.
 *
 * <h2> Thread safety </h2>
 *
 * This class assumes sequential invocation of:
 * <ul>
 <li>{@link #onCompleted()}</li>
 <li>{@link #onNext(Object)}</li>
 <li>{@link #onError(Throwable)}</li>
 </ul>
 *
 * The following methods can be called concurrently with any other method:
 * <ul>
 <li>{@link #unsubscribe()}</li>
 <li>{@link #request(long)}</li>
 </ul>
 *
 * <h2>Backpressure</h2>
 *
 * This subscriber is backpressure enabled and uses the following strategy:
 * <ul>
 <li>If the requested items from downstream are more than the items emitted, then invoke the original
 {@link Subscriber}</li>
 <li>If the requested items from downstream are less than the items emitted, then throw
 {@link MissingBackpressureException}.</li>
 <li>Whenever supply increases demand, turn off {@link ChannelConfig#isAutoRead()}.</li>
 <li>For every call to {@link Producer#request(long)} if the downstream subscriber requires more data and channel does
 not have {@link ChannelConfig#isAutoRead()} set to {@code true}, then trigger a {@link Channel#read()}</li>
 <li>The data producer of this subscriber (one calling {@link #onNext(Object)}) must make sure to call
 {@link Channel#read()} on receiving {@link ChannelInboundHandler#channelReadComplete(ChannelHandlerContext)} event and
 if {@link #shouldReadMore()} returns {@code true} </li>
 </ul>
 *
 * @author Nitesh Kant
 */
@Beta
public final class CollaboratedReadInputSubscriber<I> extends Subscriber<I> {

    private final Channel channel;

    /**
     * This is to protect agains duplicate terminal notifications, this will not necessarily mean original subsciber is
     * unsubscribed.
     */
    private boolean terminated;
    private final NotificationLite<I> nl = NotificationLite.instance();

    private volatile boolean draining; // Single threaded access

    private final Subscriber<? super I> original;
    private final Runnable fireDrainOnRequestMoreTask;


    private CollaboratedReadInputSubscriber(final Channel channel, Subscriber<? super I> op) {
        super(op);
        this.channel = channel;

        fireDrainOnRequestMoreTask = new Runnable() {
            @Override
            public void run() {
                /*
                 * The below code has to be run on the eventloop as there is a race between if(registered) and fire
                 * event. On unregistration, the pipeline is cleared and hence the event will never propagate to the
                 * handler.
                 */
                if (channel.isRegistered()) {
                    //channel.pipeline().fireUserEventTriggered(DrainInputSubscriberBuffer.INSTANCE);
                } else {
                    //drain();
                }

                /*
                 * Since, this operation has to be after the drain (to check if drain caused was sufficient for the
                 * subscriber), it is done in this task.
                 */
                if (!channel.config().isAutoRead() && shouldReadMore()) {
                    /*If the draining exhausted requested, then don't trigger read.*/
                    channel.read();
                }
            }
        };

        original = op;
        original.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                /* When unsubscribed, drain the buffer to discard buffered items */
                channel.eventLoop().execute(fireDrainOnRequestMoreTask);
            }
        }));
    }

    @Override
    public void setProducer(Producer producer) {
        /*
         * Delegate all backpressure request logic to the actual subscriber.
         */
        original.setProducer(producer);
    }

    @Override
    public void onCompleted() {
        if (terminated) {
            return;
        }
        terminated = true;
        original.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        if (terminated) {
            return;
        }
        terminated = true;
        original.onError(e);
    }

    @Override
    public void onNext(I item) {
/*
        if (terminated) {
            ReferenceCountUtil.release(item); // discard item if no one is subscribed.
        } else if (requested > 0) {
            invokeOnNext(item);
        } else {
            if (channel.config().isAutoRead()) {
                */
/*
                 * If auto-read was on then turn it off on buffer start as there is no reason to read and buffer data.
                 * If, it is desired to not turn off auto-read then the downstream subscriber should request larger
                 * number of items.
                 *//*

                channel.config().setAutoRead(false);
            }
            original.onError(new MissingBackpressureException("Received more data on the channel than demanded by the subscriber."));
        }
*/
    }

    public static <I> CollaboratedReadInputSubscriber<I> create(final Channel channel, Subscriber<? super I> original) {
        final CollaboratedReadInputSubscriber<I> toReturn = new CollaboratedReadInputSubscriber<I>(channel, original);
        toReturn.setProducer(new CRIProducer<I>(toReturn));
        return toReturn;
    }

    public boolean shouldReadMore() {
        return !terminated /*&& REQUEST_UPDATER.get(this) > 0*/;
    }

    /*Visible for testing*/ long getRequested() {
        return 0;
    }

    private void invokeOnNext(I item) {
        original.onNext(item);
        /*if (REQUEST_UPDATER.get(this) != Long.MAX_VALUE) {
            REQUEST_UPDATER.decrementAndGet(this);
        }*/
    }

    private static class CRIProducer<T> implements Producer {

        private final CollaboratedReadInputSubscriber<T> cri;

        private CRIProducer(CollaboratedReadInputSubscriber<T> cri) {
            this.cri = cri;
        }

        @Override
        public void request(long n) {
/*
            if (Long.MAX_VALUE != cri.requested) {
                if (Long.MAX_VALUE == n) {
                    // Now turning off backpressure
                    REQUEST_UPDATER.set(cri, Long.MAX_VALUE);
                } else {
                    // add n to field but check for overflow
                    while (true) {
                        long current = REQUEST_UPDATER.get(cri);
                        long next = current + n;
                        // check for overflow
                        if (next < 0) {
                            next = Long.MAX_VALUE;
                        }
                        if (REQUEST_UPDATER.compareAndSet(cri, current, next)) {
                            break;
                        }
                    }
                }
            }

            */
/*
             * Executing on the eventloop as it needs to check whether the channel is registered or not,
             * which introduces a race-condition.
             *//*

            cri.channel.eventLoop().execute(cri.fireDrainOnRequestMoreTask);
*/
        }
    }
}
