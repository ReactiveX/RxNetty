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
package io.reactivex.netty.test.util;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A handler to be added, typically to an {@link EmbeddedChannel} to simulate read from an actual channel.
 * Objects can be added to the buffer of this handler via {@link #addToTheFeed(Object...)} which will be sent to the
 * pipeline when requested via {@link #read(ChannelHandlerContext)}.
 * This handler abides by the contract of netty's backpressure semantics, to only send the number of messages as
 * requested by {@link ChannelConfig#getMaxMessagesPerRead()}, followed by
 * {@link ChannelHandlerContext#fireChannelReadComplete()}
 */
@Sharable
public class InboundRequestFeeder extends ChannelOutboundHandlerAdapter {

    private final ConcurrentLinkedQueue<Object> feed = new ConcurrentLinkedQueue<>();
    private final AtomicInteger readRequestedCount = new AtomicInteger();
    private int pendingReads;
    private boolean sending;
    private ChannelHandlerContext ctx;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().config().setAutoRead(false);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;

        readRequestedCount.incrementAndGet();
        pendingReads++;

        _sendMessages(ctx);
    }

    public void addToTheFeed(Object... msgs) {
        if (null != msgs && msgs.length > 0) {
            Collections.addAll(feed, msgs);
            if (pendingReads > 0) {
                _sendMessages(ctx);
            }
        }
    }

    private void _sendMessages(ChannelHandlerContext ctx) {
        if (sending) {
            return;
        }
        sending = true;
        int sentInThisIteration = 0;
        while (true) {
            Object next = feed.poll();
            if (null == next) {
                break;
            }
            sentInThisIteration++;
            ctx.fireChannelRead(next);

            if (sentInThisIteration >= ctx.channel().config().getMaxMessagesPerRead()) {
                sentInThisIteration = 0;
                pendingReads--;
                ctx.fireChannelReadComplete();
            }

            if (pendingReads <= 0) {
                break;
            }
        }

        sending = false;
    }

    public int getReadRequestedCount() {
        return readRequestedCount.get();
    }

    public int resetReadRequested() {
        return readRequestedCount.getAndSet(0);
    }
}
