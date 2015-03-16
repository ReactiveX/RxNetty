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
package io.reactivex.netty.channel;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BackpressureManagingHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(BackpressureManagingHandler.class);

    private enum State {
        ReadMore,
        Buffering,
        DrainingBuffer,
        Stopped,
    }

    private RecyclableArrayList buffer;
    private int currentBufferIndex;
    private State currentState = State.ReadMore;
    private boolean continueDraining;

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (currentState) {
        case ReadMore:
            newMessage(ctx, msg);
            channelReadComplete(ctx);
            break;
        case Buffering:
        case DrainingBuffer:
            if (null == buffer) {
                buffer = RecyclableArrayList.newInstance();
            }
            buffer.add(msg);
            break;
        case Stopped:
            logger.warn("Message read after handler removed, discarding the same. Message class: "
                        + msg.getClass().getName());
            ReferenceCountUtil.release(msg);
            break;
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        currentState = State.ReadMore;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        currentState = State.Stopped;
        if (null != buffer) {
            if (!buffer.isEmpty()) {
                for (Object item : buffer) {
                    ReferenceCountUtil.release(item);
                }
            }
            buffer.recycle();
        }
    }

    @Override
    public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        switch (currentState) {
        case ReadMore:
            /*
             * After read completion, move to Buffering, unless an explicit read is issued, which moves to an
             * appropriate state.
             */
            currentState = State.Buffering;
            break;
        case Buffering:
            /*Keep buffering, unless the buffer drains and more items are requested*/
            break;
        case DrainingBuffer:
            /*Keep draining, unless the buffer drains and more items are requested*/
            break;
        case Stopped:
            break;
        }

        ctx.fireChannelReadComplete();

        if (!ctx.channel().config().isAutoRead() && shouldReadMore(ctx)) {
            read(ctx);
        }
    }

    @Override
    public final void read(ChannelHandlerContext ctx) throws Exception {
        switch (currentState) {
        case ReadMore:
            /*
             * Since, current state is read more, this read signal is redundant as we do not know how much to read.
             * Pass the signal downstream, for any other handler to take a decision
             */
            ctx.read();
            break;
        case Buffering:
            /*
             * We were buffering and now a read was requested, so start draining the buffer.
             */
            currentState = State.DrainingBuffer;
            continueDraining = true;
            /*
             * Looping here to drain, instead of having it done via readComplete -> read -> readComplete loop to reduce
             * call stack depth. Otherwise, the stackdepth is proportional to number of items in the buffer and hence
             * for large buffers will overflow stack.
             */
            while (continueDraining && null != buffer && currentBufferIndex < buffer.size()) {
                Object nextItem = buffer.get(currentBufferIndex++);
                boolean shouldReadMore = shouldReadMore(ctx);
                if (!shouldReadMore) {
                    System.out.println(Thread.currentThread().getName() + ". Should read more? " + shouldReadMore);
                    new NullPointerException("Doomsday").printStackTrace();
                }
                newMessage(ctx, nextItem); /*Send the next message.*/
                //System.out.println("Sent message from the buffer");
                /*
                 * If there is more read demand then that should come as part of read complete or later as another
                 * read (this method) invocation. */
                continueDraining = false;
                channelReadComplete(ctx);
            }

            if (continueDraining) {
                if (null != buffer) {
                    /*Outstanding read demand and buffer is empty, so recycle the buffer and pass the read upstream.*/
                    buffer.recycle();
                    currentBufferIndex = 0;
                    buffer = null;
                }
                /*
                 * Since, continueDraining is true and we have broken out of the drain loop, it means that there are no
                 * items in the buffer and there is more read demand. Switch to read more and send the read demand
                 * downstream.
                 */
                currentState = State.ReadMore;
                ctx.read();
            } else {
                /*
                 * There is no more demand, so set the state to buffering and so another read invocation can start
                 * draining.
                 */
                currentState = State.Buffering;
            }
            break;
        case DrainingBuffer:
            /*Already draining buffer, so break the call stack and let the caller keep draining.*/
            continueDraining = true;
            break;
        case Stopped:
            /*Invalid, pass it downstream.*/
            ctx.read();
            break;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof RequestReadIfRequiredEvent) {
            RequestReadIfRequiredEvent requestReadIfRequiredEvent = (RequestReadIfRequiredEvent) evt;
            if (requestReadIfRequiredEvent.shouldReadMore(ctx)) {
                read(ctx);
            }
        }

        super.userEventTriggered(ctx, evt);
    }

    protected abstract void newMessage(ChannelHandlerContext ctx, Object msg);

    protected abstract boolean shouldReadMore(ChannelHandlerContext ctx);

    protected static abstract class RequestReadIfRequiredEvent {

        protected abstract boolean shouldReadMore(ChannelHandlerContext ctx);
    }

}
