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
package io.reactivex.netty.protocol.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.PrimitiveConversionHandler;
import io.reactivex.netty.protocol.tcp.BackpressureManagingHandler.BytesWriteInterceptor;
import io.reactivex.netty.protocol.tcp.BackpressureManagingHandler.RequestReadIfRequiredEvent;
import io.reactivex.netty.protocol.tcp.BackpressureManagingHandler.State;
import io.reactivex.netty.test.util.InboundRequestFeeder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class BackpressureManagingHandlerTest {

    @Rule
    public final HandlerRule handlerRule = new HandlerRule();

    @Test
    public void testExactDemandAndSupply() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final String msg1 = "hello1";
        final String msg2 = "hello2";
        handlerRule.feedMessagesForRead(msg1, msg2); /*Exact supply*/

        handlerRule.channel.config().setMaxMessagesPerRead(2); /*Send all msgs in one iteration*/
        handlerRule.requestMessages(2); /*Exact demand*/

        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg1, msg2);

        /*Since, the demand is met (requested 2 and got 2) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
    }

    @Test
    public void testExactDemandAndSupplyMultiRequests() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final String msg1 = "hello1";
        final String msg2 = "hello2";
        handlerRule.feedMessagesForRead(msg1, msg2); /*Exact supply*/

        handlerRule.channel.config().setMaxMessagesPerRead(2); /*Send all msgs in one iteration*/
        handlerRule.requestMessages(2); /*Exact demand*/

        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg1, msg2);

        /*Since, the demand is met (requested 2 and got 2) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        handlerRule.resetReadCount();
        assertThat("Unexpected read requested count post reset.", handlerRule.getReadRequestedCount(), is(0));

        handlerRule.handler.reset();

        final String msg3 = "hello3";
        handlerRule.feedMessagesForRead(msg3);

        /*No demand, no read fired*/
        assertThat("Unexpected read requested count post reset.", handlerRule.getReadRequestedCount(), is(0));
        handlerRule.assertMessagesReceived();

        handlerRule.requestMessages(1);

        /*Read on demand*/
        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg3);

        /*Since, the demand is met (requested 3 and got 3) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
    }

    @Test
    public void testMoreDemand() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final String msg1 = "hello1";
        final String msg2 = "hello2";
        handlerRule.feedMessagesForRead(msg1, msg2); /*less supply*/

        handlerRule.channel.config().setMaxMessagesPerRead(2); /*Send all msgs in one iteration*/
        handlerRule.requestMessages(4); /*More demand*/

        /*One read for start and one when the supply completed but demand exists.*/
        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(2));
        handlerRule.assertMessagesReceived(msg1, msg2);

        /*Since, the demand is not met (requested 4 but got 2) , stay in read requested.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.ReadRequested));
    }

    @Test
    public void testMoreSupply() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final String msg1 = "hello1";
        final String msg2 = "hello2";
        final String msg3 = "hello3";
        handlerRule.feedMessagesForRead(msg1, msg2, msg3); /*more supply*/

        handlerRule.channel.config().setMaxMessagesPerRead(3); /*Send all msgs in one iteration*/
        handlerRule.requestMessages(2); /*less demand*/

        /*One read for start.*/
        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg1, msg2);

        /*Since, the demand was met (requested 2 and got 2) , but the supply was more (3), we should be buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), hasSize(1));
        assertThat("Unexpected buffer contents.", handlerRule.handler.getBuffer(), contains((Object) msg3));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));
    }

    @Test
    public void testBufferDrainSingleIteration() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final String msg1 = "hello1";
        final String msg2 = "hello2";
        final String msg3 = "hello3";
        handlerRule.feedMessagesForRead(msg1, msg2, msg3); /*more supply*/

        handlerRule.channel.config().setMaxMessagesPerRead(3); /*Send all msgs in one iteration & cause buffer*/
        handlerRule.requestMessages(2); /*less demand*/

        /*One read for start.*/
        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg1, msg2);

        /*Since, the demand was met (requested 2 and got 2) , but the supply was more (3), we should be buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), hasSize(1));
        assertThat("Unexpected buffer contents.", handlerRule.handler.getBuffer(), contains((Object) msg3));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));

        handlerRule.resetReadCount();
        assertThat("Unexpected read requested count post reset.", handlerRule.getReadRequestedCount(), is(0));

        handlerRule.handler.reset();

        handlerRule.requestMessages(1); /*Should come from the buffer.*/

        assertThat("Unexpected read requested when expected to be fed from buffer.",
                   handlerRule.getReadRequestedCount(), is(0));
        handlerRule.assertMessagesReceived(msg3);

        /*Since, the demand is now met (requested 3 and got 3) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), is(nullValue()));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));
    }

    @Test
    public void testBufferDrainMultiIteration() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final String msg1 = "hello1";
        final String msg2 = "hello2";
        final String msg3 = "hello3";
        final String msg4 = "hello4";
        handlerRule.feedMessagesForRead(msg1, msg2, msg3, msg4); /*more supply*/

        handlerRule.channel.config().setMaxMessagesPerRead(4); /*Send all msgs in one iteration & cause buffer*/
        handlerRule.requestMessages(2); /*less demand*/

        /*One read for start.*/
        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg1, msg2);

        /*Since, the demand was met (requested 2 and got 2) , but the supply was more (4), we should be buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), hasSize(2));
        assertThat("Unexpected buffer contents.", handlerRule.handler.getBuffer(), contains((Object) msg3, msg4));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));

        /*Reset read state before next read*/
        handlerRule.resetReadCount();
        assertThat("Unexpected read requested count post reset.", handlerRule.getReadRequestedCount(), is(0));
        handlerRule.handler.reset();

        handlerRule.requestMessages(1); /*Should come from the buffer.*/

        assertThat("Unexpected read requested when expected to be fed from buffer.",
                   handlerRule.getReadRequestedCount(), is(0));
        handlerRule.assertMessagesReceived(msg3);

        /*Since, the demand is now met (requested 3 and got 3) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        /*Buffer does not change till it has data*/
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), hasSize(2));
        /*Buffer reader index changes till it has data*/
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(1));

        /*Reset read state before next read*/
        handlerRule.resetReadCount();
        assertThat("Unexpected read requested count post reset.", handlerRule.getReadRequestedCount(), is(0));
        handlerRule.handler.reset();

        handlerRule.requestMessages(1); /*Should come from the buffer.*/

        assertThat("Unexpected read requested when expected to be fed from buffer.",
                   handlerRule.getReadRequestedCount(), is(0));
        handlerRule.assertMessagesReceived(msg4);

        /*Since, the demand is now met (requested 4 and got 4) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), is(nullValue()));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));
    }

    @Test
    public void testBufferDrainWithMoreDemand() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final String msg1 = "hello1";
        final String msg2 = "hello2";
        final String msg3 = "hello3";
        handlerRule.feedMessagesForRead(msg1, msg2, msg3); /*more supply*/

        handlerRule.channel.config().setMaxMessagesPerRead(3); /*Send all msgs in one iteration & cause buffer*/
        handlerRule.requestMessages(2); /*less demand*/

        /*One read for start.*/
        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg1, msg2);

        /*Since, the demand was met (requested 2 and got 2) , but the supply was more (3), we should be buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), hasSize(1));
        assertThat("Unexpected buffer contents.", handlerRule.handler.getBuffer(), contains((Object) msg3));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));

        handlerRule.resetReadCount();
        assertThat("Unexpected read requested count post reset.", handlerRule.getReadRequestedCount(), is(0));

        handlerRule.handler.reset();

        handlerRule.requestMessages(2); /*Should come from the buffer.*/

        /*Since demand can not be fulfilled by the buffer, a read should be requested.*/
        assertThat("Unexpected read requested.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg3);

        /*Since, the demand is now met (requested 3 and got 3) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.ReadRequested));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), is(nullValue()));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));
    }

    @Test
    public void testBufferDrainOnRemove() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        final ByteBuf msg1 = Unpooled.buffer().writeBytes("hello1".getBytes());
        final ByteBuf msg2 = Unpooled.buffer().writeBytes("hello2".getBytes());
        handlerRule.feedMessagesForRead(msg1, msg2); /*More supply then demand*/

        handlerRule.channel.config().setMaxMessagesPerRead(2); /*Send all msgs in one iteration and cause buffer*/
        handlerRule.requestMessages(1); /*Less demand*/

        assertThat("Unexpected read requested count.", handlerRule.getReadRequestedCount(), is(1));
        handlerRule.assertMessagesReceived(msg1);

        /*Since, the demand is met (requested 1 and got 1) , we move to buffering.*/
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), hasSize(1));
        assertThat("Unexpected buffer contents.", handlerRule.handler.getBuffer(), contains((Object) msg2));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));

        handlerRule.channel.close(); // Should remove handler.
        handlerRule.channel.runPendingTasks();

        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Stopped));
        assertThat("Unexpected buffer size.", handlerRule.handler.getBuffer(), is(nullValue()));
        assertThat("Unexpected buffer read index.", handlerRule.handler.getCurrentBufferIndex(), is(0));
        assertThat("Buffered item not released.", msg2.refCnt(), is(0));
    }

    @Test
    public void testDiscardReadWhenStopped() throws Exception {
        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Buffering));

        handlerRule.channel.close(); // Should remove handler.
        handlerRule.channel.runPendingTasks();

        assertThat("Unexpected handler state.", handlerRule.handler.getCurrentState(), is(State.Stopped));

        final ByteBuf msg = Unpooled.buffer().writeBytes("Hello".getBytes());
        handlerRule.handler.channelRead(Mockito.mock(ChannelHandlerContext.class), msg);

        assertThat("Message not released when stopped.", msg.refCnt(), is(0));
    }

    @Test
    public void testWriteWithBufferingHandler() throws Exception {
        BufferingHandler bufferingHandler = new BufferingHandler();
        handlerRule.channel.pipeline()
                           .addBefore(BytesWriteInterceptor.WRITE_INSPECTOR_HANDLER_NAME, "buffering-handler",
                                      bufferingHandler);

        final String[] dataToWrite = {"Hello1", "Hello2"};

        handlerRule.channel.writeAndFlush(Observable.from(dataToWrite));/*Using Observable.from() to enable backpressure.*/

        assertThat("Messages written to the channel, inspite of buffering", handlerRule.channel.outboundMessages(),
                   is(empty()));

        /*Inspite of the messages, not reaching the channel, the extra demand should be generated and the buffering
        handler should contain all messages.*/
        assertThat("Unexpected buffer size in buffering handler.", bufferingHandler.buffer, hasSize(2));
    }

    public static class HandlerRule extends ExternalResource {

        private MockBackpressureManagingHandler handler;
        private EmbeddedChannel channel;
        private InboundRequestFeeder inboundRequestFeeder;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    inboundRequestFeeder = new InboundRequestFeeder();
                    channel = new EmbeddedChannel();
                    String bpName = "backpressure-manager";
                    channel.pipeline().addFirst(bpName,
                                                handler = new MockBackpressureManagingHandler(bpName));
                    channel.pipeline().addBefore(bpName, "primitive-converter", PrimitiveConversionHandler.INSTANCE);
                    channel.pipeline().addFirst(inboundRequestFeeder);
                    channel.config().setAutoRead(false);
                    base.evaluate();
                }
            };
        }

        public void assertMessagesReceived(Object... expected) {
            final List<Object> msgsReceived = handler.getMsgsReceived();

            if (null != expected && expected.length > 0) {
                assertThat("Unexpected messages received count.", msgsReceived, hasSize(expected.length));
                assertThat("Unexpected messages received.", msgsReceived, contains(expected));
            } else {
                assertThat("Unexpected messages received.", msgsReceived, is(empty()));
            }
        }

        public int resetReadCount() {
            return inboundRequestFeeder.resetReadRequested();
        }

        public int getReadRequestedCount() {
            return inboundRequestFeeder.getReadRequestedCount();
        }

        public void requestMessages(long requested) throws Exception {
            handler.incrementRequested(requested);
            channel.pipeline().fireUserEventTriggered(new RequestReadIfRequiredEvent() {
                @Override
                protected boolean shouldReadMore(ChannelHandlerContext ctx) {
                    return true;
                }
            });
            channel.runPendingTasks();
        }

        public void feedMessagesForRead(Object... msgs) {
            inboundRequestFeeder.addToTheFeed(msgs);
        }
    }

    private static class MockBackpressureManagingHandler extends BackpressureManagingHandler {

        private final List<Object> msgsReceived = new ArrayList<>();
        private final AtomicLong requested = new AtomicLong();

        protected MockBackpressureManagingHandler(String thisHandlerName) {
            super(thisHandlerName);
        }

        @Override
        protected void newMessage(ChannelHandlerContext ctx, Object msg) {
            requested.decrementAndGet();
            msgsReceived.add(msg);
        }

        @Override
        protected boolean shouldReadMore(ChannelHandlerContext ctx) {
            return requested.get() > 0;
        }

        public List<Object> getMsgsReceived() {
            return msgsReceived;
        }

        public void reset() {
            msgsReceived.clear();
            requested.set(0);
        }

        public void incrementRequested(long requested) {
            this.requested.addAndGet(requested);
        }
    }

    private static class BufferingHandler extends ChannelOutboundHandlerAdapter {

        private final List<Object> buffer = new ArrayList<>();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            buffer.add(msg);
        }
    }
}