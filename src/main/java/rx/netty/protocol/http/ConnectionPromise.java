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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * A {@link Promise} that gets set when a connection is established.
 * 
 * @param <T>
 *            The type of response messages.
 * @param <R>
 *            The request type
 */
class ConnectionPromise<T, R extends HttpRequest> extends DefaultPromise<RequestWriter<T, R>> {
    private EventExecutor executor;
    private Channel channel;
    private HttpProtocolHandler<T> handler;

    ConnectionPromise(EventExecutor executor, HttpProtocolHandler<T> handler) {
        this.executor = executor;
        this.handler = handler;
    }

    @Override
    protected EventExecutor executor() {
        return this.executor;
    }

    public Channel channel() {
        return channel;
    }

    void onConnect(Channel channel) {
        this.executor = channel.eventLoop();
        this.channel = channel;
        handler.configure(channel.pipeline());
        trySuccess(new RequestWriter<T, R>(channel));
    }
}
