/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

/**
 * An observable connection for connection oriented protocols.
 *
 * @param <I> The type of the object that is read from this connection.
 * @param <O> The type of objects that are written to this connection.
 */
public class ObservableConnection<I, O> {

    private final PublishSubject<I> s;
    private final ChannelHandlerContext ctx;

    protected ObservableConnection(final ChannelHandlerContext ctx, final PublishSubject<I> s) {
        this.ctx = ctx;
        this.s = s;
    }

    public static <I, O> ObservableConnection<I, O> create(ChannelHandlerContext ctx) {
        return new ObservableConnection<I, O>(ctx, PublishSubject.<I> create());
    }

    public Observable<I> getInput() {
        return s;
    }

    PublishSubject<I> getInputPublishSubject() {
        return s;
    }

    /**
     * Writes the passed {@code msg} to the underlying channel immediately. <br/>
     * Every new subscription will not invoke the write i.e. it is an eager write.
     * 
     * @param msg Message to write.
     *
     * @return The observable associated with the completion of the write action.
     */
    public Observable<Void> writeNow(final O msg) {
        final ChannelFuture f = ctx.writeAndFlush(msg);

        Observable<Void> o = Observable.create(new OnSubscribeFunc<Void>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Void> observer) {
                f.addListener(new GenericFutureListener<Future<Void>>() {

                    @Override
                    public void operationComplete(Future<Void> future) throws Exception {
                        if (future.isSuccess()) {
                            observer.onCompleted();
                        } else {
                            observer.onError(future.cause());
                        }
                    }
                });

                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        f.cancel(true);
                    }
                });
            }
        });

        // return Observable that is multicast (cached) so it can be subscribed to it wanted
        return o;

    }
}
