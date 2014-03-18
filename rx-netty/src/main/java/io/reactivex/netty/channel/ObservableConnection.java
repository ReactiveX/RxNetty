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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.reactivex.netty.client.pool.ChannelPool;
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An observable connection for connection oriented protocols.
 *
 * @param <I> The type of the object that is read from this connection.
 * @param <O> The type of objects that are written to this connection.
 */
public class ObservableConnection<I, O> extends DefaultChannelWriter<O> {

    private static final Observable<Void> CONNECTION_ALREADY_CLOSED =
            Observable.error(new IllegalStateException("Connection is already closed."));
    private final PublishSubject<I> inputSubject;
    private final AtomicBoolean closeIssued = new AtomicBoolean();

    public ObservableConnection(final ChannelHandlerContext ctx, final PublishSubject<I> inputSubject) {
        super(ctx);
        this.inputSubject = inputSubject;
    }

    public Observable<I> getInput() {
        return inputSubject;
    }

    /**
     * Closes this connection. This method is idempotent, so it can be called multiple times without any side-effect on
     * the channel.
     *
     * @return Observable signifying the close on the connection. Returns {@link Observable#error(Throwable)} if the
     * close is already issued (may not be completed)
     */
    public Observable<Void> close() {
        final ChannelFuture closeFuture;
        if (closeIssued.compareAndSet(false, true)) {
            inputSubject.onCompleted();
            ChannelPool pool = getChannelHandlerContext().channel().attr(ChannelPool.POOL_ATTR).get();
            if (pool == null) {
                ReadTimeoutPipelineConfigurator.removeTimeoutHandler(getChannelHandlerContext().pipeline());
                closeFuture = getChannelHandlerContext().close();
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
            } else {
                return pool.releaseChannel(getChannelHandlerContext().channel());
            }
        } else {
            return CONNECTION_ALREADY_CLOSED;
        }
    }
}
