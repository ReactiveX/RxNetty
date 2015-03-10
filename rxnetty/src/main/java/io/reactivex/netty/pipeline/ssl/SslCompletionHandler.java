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
package io.reactivex.netty.pipeline.ssl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import rx.Observable;
import rx.subjects.ReplaySubject;

import javax.net.ssl.SSLException;

/**
 * An extension of {@link SslHandler} to provide a single {@link Observable} for SSL process completion via
 * {@link #sslCompletionStatus()}.
 *
 * @author Nitesh Kant
 */
public class SslCompletionHandler extends ChannelDuplexHandler {

    private final ReplaySubject<Void> sslCompletionStatus;

    public SslCompletionHandler(final Future<Channel> sslHandshakeFuture) {
        sslCompletionStatus = ReplaySubject.create();
        sslHandshakeFuture.addListener(new GenericFutureListener<Future<? super Channel>>() {
            @Override
            public void operationComplete(Future<? super Channel> future) throws Exception {
                if (future.isSuccess()) {
                    sslCompletionStatus.onCompleted();
                } else {
                    sslCompletionStatus.onError(future.cause());
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SSLException) {
            sslCompletionStatus.onError(cause);
        }
        super.exceptionCaught(ctx, cause);
    }

    public Observable<Void> sslCompletionStatus() {
        return sslCompletionStatus;
    }
}
