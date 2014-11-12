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

package io.reactivex.netty.util;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import rx.Observable;
import rx.Subscriber;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nitesh Kant
 */
public class MultipleFutureListener implements ChannelFutureListener {

    private final ChannelPromise completionPromise;
    private final Observable<Void> completionObservable;
    private final AtomicInteger listeningToCount = new AtomicInteger();
    private final ConcurrentLinkedQueue<ChannelFuture> pendingFutures = new ConcurrentLinkedQueue<ChannelFuture>();

    public MultipleFutureListener(final ChannelPromise completionPromise) {
        if (null == completionPromise) {
            throw new NullPointerException("Promise can not be null.");
        }
        this.completionPromise = completionPromise;
        completionObservable = Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                if (listeningToCount.get() == 0) {
                    MultipleFutureListener.this.completionPromise.trySuccess();
                }
                MultipleFutureListener.this.completionPromise.addListener(new ChannelFutureListener() {
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

    public void listen(ChannelFuture future) {
        pendingFutures.add(future);
        listeningToCount.incrementAndGet();
        future.addListener(this);
    }

    public Observable<Void> asObservable() {
        return completionObservable;
    }

    public void cancelPendingFutures(boolean mayInterruptIfRunning) {
        for (Iterator<ChannelFuture> iterator = pendingFutures.iterator(); iterator.hasNext(); ) {
            ChannelFuture pendingFuture = iterator.next();
            iterator.remove();
            pendingFuture.cancel(mayInterruptIfRunning);
        }
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        pendingFutures.remove(future);
        int nowListeningTo = listeningToCount.decrementAndGet();
        /**
         * If any one of the future fails, we fail the subscribers.
         * If all complete (listen count == 0) we complete the subscribers.
         */
        if (!future.isSuccess()) {
            completionPromise.tryFailure(future.cause());
        } else if (nowListeningTo == 0) {
            completionPromise.trySuccess(null);
        }
    }
}
