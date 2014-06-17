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
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

/**
 * An observable connection for connection oriented protocols.
 *
 * @param <I> The type of the object that is read from this connection.
 * @param <O> The type of objects that are written to this connection.
 */
public class ObservableConnection<I, O> extends DefaultChannelWriter<O> {

    private PublishSubject<I> inputSubject;

    public ObservableConnection(final ChannelHandlerContext ctx) {
        super(ctx);
        inputSubject = PublishSubject.create();
        ChannelHandlerContext firstContext = ctx.pipeline().firstContext();
        firstContext.fireUserEventTriggered(new NewRxConnectionEvent(inputSubject));
    }

    public Observable<I> getInput() {
        return inputSubject;
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
    protected Observable<Void> _close() {
        PublishSubject<I> thisSubject = inputSubject;
        cleanupConnection();
        Observable<Void> toReturn = _closeChannel();
        thisSubject.onCompleted(); // This is just to make sure we make the subject as completed after we finish
        // closing the channel, results in more deterministic behavior for clients.
        return toReturn;
    }

    protected void cleanupConnection() {
        cancelPendingWrites(true);
        ReadTimeoutPipelineConfigurator.removeTimeoutHandler(getChannelHandlerContext().pipeline());
    }

    protected Observable<Void> _closeChannel() {
        final ChannelFuture closeFuture = getChannelHandlerContext().close();
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

    protected void updateInputSubject(PublishSubject<I> newSubject) {
        inputSubject = newSubject;
    }
}
