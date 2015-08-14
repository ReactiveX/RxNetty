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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * A bridge to connect a {@link Subscriber} to a {@link ChannelFuture} so that when the {@code subscriber} is
 * unsubscribed, the listener will get removed from the {@code future}. Failure to do so for futures that are long
 * living, eg: {@link Channel#closeFuture()} will lead to a memory leak where the attached listener will be in the
 * listener queue of the future till the channel closes.
 *
 * In order to bridge the future and subscriber, {@link #bridge(ChannelFuture, Subscriber)} must be called.
 */
public abstract class SubscriberToChannelFutureBridge implements ChannelFutureListener {

    @Override
    public final void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            doOnSuccess(future);
        } else {
            doOnFailure(future, future.cause());
        }
    }

    protected abstract void doOnSuccess(ChannelFuture future);

    protected abstract void doOnFailure(ChannelFuture future, Throwable cause);

    /**
     * Bridges the passed subscriber and future, which means the following:
     *
     * <ul>
     <li>Add this listener to the passed future.</li>
     <li>Add a callback to the subscriber, such that on unsubscribe this listener is removed from the future.</li>
     </ul>
     *
     * @param future Future to bridge.
     * @param subscriber Subscriber to connect to the future.
     */
    public void bridge(final ChannelFuture future, Subscriber<?> subscriber) {
        future.addListener(this);
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                future.removeListener(SubscriberToChannelFutureBridge.this);
            }
        }));
    }
}
