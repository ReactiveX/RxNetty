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
package io.reactivex.netty.protocol.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * A {@link Promise} that gets called when an HTTP transaction is done. If the HTTP transaction
 * returns a full http response, this promise will contain that object too.
 * 
 * @param <T>
 * @param <R>
 */
public class RequestCompletionPromise<T, R extends HttpRequest> extends DefaultPromise<T> {

    private final Future<RequestWriter<T, R>> connectionFuture;
    private Future<T> requestProcessingFuture;
    private EventExecutor executor;

    public RequestCompletionPromise(EventExecutor _executor, Future<RequestWriter<T, R>> clientGetFuture) {
        this.executor = _executor;
        this.connectionFuture = clientGetFuture;
    }

    @Override
    protected EventExecutor executor() {
        return this.executor;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (connectionFuture.isCancellable()) {
            connectionFuture.cancel(mayInterruptIfRunning);
        } else if (null != requestProcessingFuture && requestProcessingFuture.isCancellable()) {
            requestProcessingFuture.cancel(mayInterruptIfRunning);
        }

        return super.cancel(mayInterruptIfRunning);
    }

    void setRequestProcessingFuture(Channel channel, Future<T> requestProcessFuture) {
        this.executor = channel.eventLoop();
        this.requestProcessingFuture = requestProcessFuture;
        this.requestProcessingFuture.addListener(new GenericFutureListener<Future<T>>() {
            @Override
            public void operationComplete(Future<T> future) throws Exception {
                if (future.isSuccess()) {
                    setSuccess(future.get());
                } else {
                    setFailure(future.cause());
                }
            }
        });
    }
}