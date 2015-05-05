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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.BackpressureManagingHandler.WriteStreamSubscriber;
import io.reactivex.netty.test.util.MockProducer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.Queue;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class WriteStreamSubscriberTest {

    @Rule
    public final SubscriberRule subscriberRule = new SubscriberRule();

    @Test
    public void testOnStart() throws Exception {
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));
        subscriberRule.start();
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));

        assertThat("Unexpected request made to the producer.", subscriberRule.mockProducer.getRequested(), is(1L));
    }

    @Test
    public void testUnsubscribeOnPromiseCancel() throws Exception {
        subscriberRule.start();

        assertThat("Subsriber isn't subscribed.", subscriberRule.subscriber.isUnsubscribed(), is(false));

        subscriberRule.channelPromise.cancel(false);

        assertThat("Promise not cancelled.", subscriberRule.channelPromise.isCancelled(), is(true));

        assertThat("Subsriber isn't unsubscribed.", subscriberRule.subscriber.isUnsubscribed(), is(true));
    }

    @Test
    public void testWriteCompleteBeforeStream() throws Exception {
        subscriberRule.start();

        String msg1 = "msg1";
        subscriberRule.writeAndFlushMessages(msg1);
        subscriberRule.assertMessagesWritten(msg1);

        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));

        subscriberRule.subscriber.onCompleted();

        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(true));
        assertThat("Unexpected promise result.", subscriberRule.channelPromise.isSuccess(), is(true));
    }

    @Test
    public void testWriteCompleteAfterStream() throws Exception {
        subscriberRule.start();

        String msg1 = "msg1";
        subscriberRule.writeMessages(msg1);
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));
        subscriberRule.subscriber.onCompleted();
        /*Complete when write completes.*/
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));

        subscriberRule.channel.flush(); /*Completes write*/

        subscriberRule.assertMessagesWritten(msg1);

        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(true));
        assertThat("Unexpected promise result.", subscriberRule.channelPromise.isSuccess(), is(true));
    }

    @Test
    public void testMultiWrite() throws Exception {
        subscriberRule.start();

        String msg1 = "msg1";
        String msg2 = "msg2";
        subscriberRule.writeMessages(msg1, msg2);
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));
        subscriberRule.subscriber.onCompleted();
        /*Complete when write completes.*/
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));

        subscriberRule.channel.flush(); /*Completes write*/

        subscriberRule.assertMessagesWritten(msg1, msg2);

        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(true));
        assertThat("Unexpected promise result.", subscriberRule.channelPromise.isSuccess(), is(true));
    }

    @Test
    public void testWriteFailed() throws Exception {
        subscriberRule.start();

        String msg1 = "msg1";
        subscriberRule.writeMessages(msg1);
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));

        subscriberRule.channel.close();

        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(true));
        assertThat("Unexpected promise result.", subscriberRule.channelPromise.isSuccess(), is(false));
    }

    @Test
    public void testStreamError() throws Exception {
        subscriberRule.start();

        String msg1 = "msg1";
        subscriberRule.writeAndFlushMessages(msg1);
        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(false));
        subscriberRule.assertMessagesWritten(msg1);

        subscriberRule.subscriber.onError(new IOException());

        assertThat("Unexpected promise completion state.", subscriberRule.channelPromise.isDone(), is(true));
        assertThat("Unexpected promise result.", subscriberRule.channelPromise.isSuccess(), is(false));
    }

    public static class SubscriberRule extends ExternalResource {

        private WriteStreamSubscriber subscriber;
        private ChannelPromise channelPromise;
        private EmbeddedChannel channel;
        private MockProducer mockProducer;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    channel = new EmbeddedChannel();
                    channelPromise = channel.newPromise();
                    ChannelHandlerContext ctx = channel.pipeline().firstContext();
                    subscriber = new WriteStreamSubscriber(ctx, channelPromise);
                    mockProducer = new MockProducer();
                    base.evaluate();
                }
            };
        }

        public void start() {
            subscriber.onStart(); /*So that setProducer does not request Long.MAX_VALUE*/
            subscriber.setProducer(mockProducer);

            mockProducer.assertBackpressureRequested();
            mockProducer.assertIllegalRequest();
        }

        public void writeAndFlushMessages(Object... msgs) {
            writeMessages(msgs);
            channel.flush();
        }

        public void writeMessages(Object... msgs) {
            for (Object msg : msgs) {
                subscriber.onNext(msg);
            }
        }

        public void assertMessagesWritten(Object... msgs) {
            Queue<Object> outboundMessages = channel.outboundMessages();

            if (null == msgs || msgs.length == 0) {
                assertThat("Unexpected number of messages written on the channel.", outboundMessages, is(empty()));
                return;
            }

            assertThat("Unexpected number of messages written on the channel.", outboundMessages, hasSize(msgs.length));
            assertThat("Unexpected messages written on the channel.", outboundMessages, contains(msgs));
        }
    }
}