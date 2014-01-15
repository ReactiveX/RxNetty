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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * The listener that gets called when an HTTP connection is established. It sends a request over the established connection,
 * and then sets up proper listeners for incoming responses
 * 
 * @param <T>
 *            The type of returned responses.
 * @param <R>
 *            The type of outgoing request. It has to be a subclass of {@link HttpRequest}.
 */
class ConnectionListener<T, R extends HttpRequest> implements GenericFutureListener<Future<RequestWriter<T, R>>> {
    private final ConnectionPromise connectionPromise;

    private final R request;
    private final RequestCompletionPromise<T, R> requestCompletionPromise;

    public ConnectionListener(ConnectionPromise connectionPromise, R request, RequestCompletionPromise<T, R> requestPromise) {
        this.connectionPromise = connectionPromise;
        this.request = request;
        this.requestCompletionPromise = requestPromise;
    }

    @Override
    public void operationComplete(Future<RequestWriter<T, R>> future) throws Exception {
        if (future.isSuccess()) {
            RequestWriter<T, R> requestWriter = future.get();
            Future<T> requestProcessingFuture = requestWriter.execute(request, requestCompletionPromise);
            requestCompletionPromise.setRequestProcessingFuture(channel(), requestProcessingFuture);
        } else {
            requestCompletionPromise.setFailure(future.cause());
        }
    }

    private Channel channel() {
        return connectionPromise.channel();
    }

}
