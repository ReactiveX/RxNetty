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
package io.reactivex.netty.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.reactivex.netty.channel.ChannelMetricEventProvider;

/**
 * @author Nitesh Kant
 */
public class BytesInspector extends ChannelDuplexHandler {

    @SuppressWarnings("rawtypes")private final MetricEventsSubject eventsSubject;
    private final ChannelMetricEventProvider metricEventProvider;

    public BytesInspector(@SuppressWarnings("rawtypes")MetricEventsSubject eventsSubject,
                          ChannelMetricEventProvider metricEventProvider) {
        this.eventsSubject = eventsSubject;
        this.metricEventProvider = metricEventProvider;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ByteBuf.class.isInstance(msg)) {
            ByteBuf byteBuf = (ByteBuf) msg;
            eventsSubject.onEvent(metricEventProvider.getBytesReadEvent());
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // TODO: Auto-generated method stub
        super.write(ctx, msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        // TODO: Auto-generated method stub
        super.flush(ctx);
    }
}
