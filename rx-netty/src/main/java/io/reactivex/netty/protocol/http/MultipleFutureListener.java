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
package io.reactivex.netty.protocol.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nitesh Kant
 */
public class MultipleFutureListener implements ChannelFutureListener {

    private final ChannelPromise finalPromise;

    private final AtomicInteger listeningToCount = new AtomicInteger();
    private final ConcurrentLinkedQueue<ChannelFuture> pendingFutures = new ConcurrentLinkedQueue<ChannelFuture>();
    private final PublishSubject<ChannelFuture> lastCompletedFuture; // This never completes or throw an error.
    private final ChannelFuture futureWhenNoPendingFutures;

    public MultipleFutureListener(ChannelPromise finalPromise) {
        if (null == finalPromise) {
            throw new NullPointerException("Promise can not be null.");
        }
        this.finalPromise = finalPromise;
        lastCompletedFuture = PublishSubject.create();
        futureWhenNoPendingFutures = finalPromise.channel().newPromise().setSuccess();
    }

    public MultipleFutureListener(ChannelHandlerContext ctx) {
        finalPromise = null;
        lastCompletedFuture = PublishSubject.create();
        futureWhenNoPendingFutures = ctx.newPromise();
    }

    public void listen(ChannelFuture future) {
        pendingFutures.add(future);
        listeningToCount.incrementAndGet();
        future.addListener(this);
    }

    public Observable<ChannelFuture> listenForNextCompletion() {
        if (listeningToCount.get() > 0) {
            return lastCompletedFuture;
        } else {
            return Observable.<ChannelFuture>from(futureWhenNoPendingFutures);
        }
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
        if (!future.isSuccess()) {
            if (null != finalPromise) {
                cancelPendingFutures(true);
                finalPromise.tryFailure(future.cause());
            } else {
                lastCompletedFuture.onError(future.cause());
            }
        } else if (nowListeningTo <= 0) {
            if (null != finalPromise) {
                finalPromise.trySuccess(null);
            }
            lastCompletedFuture.onNext(future);
        }
    }
}
