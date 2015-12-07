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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BytesInspector extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(BytesInspector.class);

    private final ConnectionEventListener eventListener;
    private final EventPublisher eventPublisher;

    public BytesInspector(EventPublisher eventPublisher, ConnectionEventListener eventListener) {
        this.eventPublisher = eventPublisher;
        this.eventListener = eventListener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
                publishBytesRead((ByteBuf) msg);
            } else if (ByteBufHolder.class.isAssignableFrom(msg.getClass())) {
                ByteBufHolder holder = (ByteBufHolder) msg;
                publishBytesRead(holder.content());
            }
        } catch (Exception e) {
            logger.warn("Failed to publish bytes read metrics event. This does *not* stop the pipeline processing.", e);
        } finally {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
                publishBytesWritten(((ByteBuf) msg).readableBytes(), promise);
            } else if (ByteBufHolder.class.isAssignableFrom(msg.getClass())) {
                publishBytesWritten(((ByteBufHolder)msg).content().readableBytes(), promise);
            } else if (FileRegion.class.isAssignableFrom(msg.getClass())) {
                publishBytesWritten(((FileRegion) msg).count(), promise);
            }
        } catch (Exception e) {
            logger.warn("Failed to publish bytes write metrics event. This does *not* stop the pipeline processing.", e);
        } finally {
            super.write(ctx, msg, promise);
        }
    }

    @SuppressWarnings("unchecked")
    protected void publishBytesWritten(final long bytesToWrite, ChannelPromise promise) {
        if (bytesToWrite <= 0) {
            return;
        }

        if (eventPublisher.publishingEnabled()) {
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    eventListener.onByteWritten(bytesToWrite);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    protected void publishBytesRead(ByteBuf byteBuf) {
        if (null != byteBuf) {
            eventListener.onByteRead(byteBuf.readableBytes());
        }
    }
}