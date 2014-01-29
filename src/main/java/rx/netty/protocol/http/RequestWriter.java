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
package rx.netty.protocol.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;

/**
 * This class is responsible for sending a request object to the server and then setting up
 * proper listeners when sending request is successful
 * 
 * @param <T>
 *            The type of response content
 * @param <R>
 *            The type of the request
 */
class RequestWriter<T, R extends HttpRequest> {
    private final Channel channel;
    private final HttpProtocolHandler<T> handler;

    RequestWriter(Channel channel, HttpProtocolHandler<T> handler) {
        this.channel = channel;
        this.handler = handler;
    }

    Future<T> execute(R request, final RequestCompletionPromise requestCompletionPromise) {
        ChannelPromise promise = channel.newPromise();
        channel.writeAndFlush(request, promise).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    requestCompletionPromise.tryFailure(future.cause());
                }
            }
        }).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                handler.onChannelWriteOperationCompleted(future);
            }
        });
        return new RequestWrittenPromise(channel, promise);
    }

    Channel getChannel() {
        return channel;
    }

    public boolean isActive() {
        return channel.isActive();
    }

    private class RequestWrittenPromise extends DefaultPromise<T> {

        private final ChannelPromise sendRequestPromise;

        public RequestWrittenPromise(Channel channel, ChannelPromise sendRequestPromise) {
            super(channel.eventLoop());
            this.sendRequestPromise = sendRequestPromise;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (sendRequestPromise.isCancellable()) {
                sendRequestPromise.cancel(mayInterruptIfRunning);
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }
}