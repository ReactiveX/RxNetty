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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
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

    private final PublishSubject<I> inputSubject;
    private final ChannelHandlerContext ctx;

    /**
     * Thrown if write is attempted on a closed channel.
     */
    private static final IllegalStateException CHANNEL_ALREADY_CLOSED_EXCEPTION = new IllegalStateException("Channel already closed.");

    public static final Subscription DO_NOTHING_SUBSCRIPTION = Subscriptions.create(new Action0() {
        @Override
        public void call() { }
    });

    /**
     * Since, there isn't any state here in this observable, this is a static constant.
     */
    public static final Observable<Void> WRITE_WHEN_CONNECTION_CLOSED_OBSERVABLE = Observable.create(
            new OnSubscribeFunc<Void>() {
                @Override
                public Subscription onSubscribe(Observer<? super Void> observer) {
                    observer.onError(CHANNEL_ALREADY_CLOSED_EXCEPTION);
                    return DO_NOTHING_SUBSCRIPTION;
                }
            });

    public ObservableConnection(final ChannelHandlerContext ctx, final PublishSubject<I> inputSubject) {
        this.ctx = ctx;
        this.inputSubject = inputSubject;
    }

    /**
     * Writes the passed {@code msg} to the underlying channel immediately. <br/>
     * Every new subscription will not invoke the write i.e. it is an eager write.
     *
     * @param msg Message to write.
     *
     * @return The observable associated with the completion of the write action.
     */
    public Observable<Void> writeAndFlush(final O msg) {

        if (!ctx.channel().isActive()) {
            return WRITE_WHEN_CONNECTION_CLOSED_OBSERVABLE;
        }

        final ChannelFuture f = ctx.writeAndFlush(msg);

        Observable<Void> o = Observable.create(new OnSubscribeFunc<Void>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Void> observer) {
                f.addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
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

        // return Observable that is multicast (cached) so it can be subscribed to if required
        return o;
    }

    public Observable<I> getInput() {
        return inputSubject;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return ctx;
    }

    public void close() {
        inputSubject.onCompleted();
        ReadTimeoutPipelineConfigurator.removeTimeoutHandler(ctx.pipeline());
    }
}
