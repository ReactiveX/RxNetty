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
 *
 */
package io.reactivex.netty.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

/**
 * An event to indicate to {@link AbstractConnectionToChannelBridge} that the subscriber as published by
 * {@link ConnectionSubscriberEvent} should be informed of a connection creation failure, instead of a new connection.
 *
 * <h2>Why do we need this?</h2>
 *
 * Since, emitting a connection can include a handshake for protocols such as TLS/SSL, it is not so that a new
 * {@link io.reactivex.netty.channel.Connection} should be emitted as soon as the channel is active (i.e. inside
 * {@link ChannelInboundHandler#channelActive(ChannelHandlerContext)}).
 * For this reason, this event leaves it to the pipeline or any other entity outside to decide, when is the rite time to
 * determine that a connection for a channel has failed creation.
 */
public final class ConnectionCreationFailedEvent {

    private final Throwable throwable;

    public ConnectionCreationFailedEvent(Throwable throwable) {
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
