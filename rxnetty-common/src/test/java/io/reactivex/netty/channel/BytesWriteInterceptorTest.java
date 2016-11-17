/*
 * Copyright 2016 Netflix, Inc.
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

import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.BackpressureManagingHandler.BytesWriteInterceptor;
import io.reactivex.netty.channel.BackpressureManagingHandler.WriteStreamSubscriber;
import io.reactivex.netty.test.util.MockProducer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static io.reactivex.netty.channel.BackpressureManagingHandler.BytesWriteInterceptor.MAX_PER_SUBSCRIBER_REQUEST;
import static io.reactivex.netty.channel.BytesWriteInterceptorTest.InspectorRule.defaultRequestN;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class BytesWriteInterceptorTest {

    @Rule
    public final InspectorRule inspectorRule = new InspectorRule();

    @Test(timeout = 60000)
    public void testAddSubscriber() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        sub1.unsubscribe();
        inspectorRule.channel.runPendingTasks();
        assertThat("Subscriber not removed post unsubscribe", inspectorRule.interceptor.getSubscribers(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testRequestMore() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(defaultRequestN()));

        inspectorRule.sendMessages(1);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested.", mockProducer.getRequested(), is(defaultRequestN()));
    }

    @Test(timeout = 60000)
    public void testRequestMorePostFlush() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(defaultRequestN()));

        inspectorRule.channel.config().setWriteBufferWaterMark(new WriteBufferWaterMark(1, 2)); /*Make sure that the channel is not writable on writing.*/

        String msg = "Hello";
        inspectorRule.channel.write(msg);

        assertThat("Channel still writable.", inspectorRule.channel.isWritable(), is(false));
        assertThat("More items requested when channel is not writable.", mockProducer.getRequested(),
                   is(defaultRequestN()));

        inspectorRule.channel.flush();

        assertThat("Channel not writable post flush.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested.", mockProducer.getRequested(), is(defaultRequestN()));
    }

    @Test(timeout = 60000)
    public void testMultiSubscribers() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 1);

        WriteStreamSubscriber sub2 = inspectorRule.newSubscriber();
        MockProducer producer2 = inspectorRule.setupSubscriberAndValidate(sub2, 2);

        inspectorRule.sendMessages(1);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested from first subscriber.", producer1.getRequested(),
                   is(defaultRequestN()));
        assertThat("Unexpected items requested from second subscriber.", producer2.getRequested(),
                   is(defaultRequestN() / 2));
    }

    @Test(timeout = 10000)
    public void testOneLongWriteAndManySmallWrites() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", producer1.getRequested(), is(defaultRequestN()));
        inspectorRule.setupNewSubscriberAndComplete(2, true);
        inspectorRule.setupNewSubscriberAndComplete(2, true);

        inspectorRule.sendMessages(sub1, 33);
        assertThat("Unexpected items requested.", producer1.getRequested(), is(97L));
    }

    @Test(timeout = 10000)
    public void testBatchedSubscriberRemoves() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", producer1.getRequested(), is(defaultRequestN()));
        for (int i=1; i < 5; i++) {
            inspectorRule.setupNewSubscriberAndComplete(i+1, false);
        }

        inspectorRule.channel.runPendingTasks();

        inspectorRule.sendMessages(sub1, 35);
        assertThat("Unexpected items requested.", producer1.getRequested(), is(95L));
    }

    @Test(timeout = 10000)
    public void testMinRequestN() throws Exception {
        for (int i=1; i < 66; i++) {
            inspectorRule.setupNewSubscriberAndComplete(i, false);
        }
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 66);
        assertThat("Unexpected items requested from producer.", producer1.getRequested(), is(1L));

        inspectorRule.channel.runPendingTasks();
        inspectorRule.sendMessages(sub1, 35);
        assertThat("Unexpected items requested.", producer1.getRequested(), greaterThan(1L));
    }

    public static class InspectorRule extends ExternalResource {

        private BytesWriteInterceptor interceptor;
        private EmbeddedChannel channel;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    interceptor = new BytesWriteInterceptor("foo");
                    channel = new EmbeddedChannel(new WriteTransformer(), interceptor);
                    base.evaluate();
                }
            };
        }

        WriteStreamSubscriber newSubscriber() {
            return interceptor.newSubscriber(channel.pipeline().lastContext(), channel.newPromise());
        }

        private MockProducer setupSubscriberAndValidate(WriteStreamSubscriber sub, int expectedSubCount) {
            MockProducer mockProducer = setupSubscriber(sub);
            assertThat("Subscriber not added.", interceptor.getSubscribers(), hasSize(expectedSubCount));
            assertThat("Subscriber not added.", interceptor.getSubscribers().get(expectedSubCount - 1), equalTo(sub));
            return mockProducer;
        }

        private static MockProducer setupSubscriber(WriteStreamSubscriber sub) {
            sub.onStart();
            MockProducer mockProducer = new MockProducer();
            sub.setProducer(mockProducer);
            return mockProducer;
        }

        public static Long defaultRequestN() {
            return Long.valueOf(MAX_PER_SUBSCRIBER_REQUEST);
        }

        public void sendMessages(WriteStreamSubscriber subscriber, int msgCount) {
            for(int i=0; i < msgCount; i++) {
                subscriber.onNext("Hello");
                channel.write("Hello");
            }
            channel.flush();
        }

        public void sendMessages(int msgCount) {
            for(int i=0; i < msgCount; i++) {
                channel.write("Hello");
            }
            channel.flush();
        }

        public void setupNewSubscriberAndComplete(int expectedSubCount, boolean runPendingTasks) {
            WriteStreamSubscriber sub2 = newSubscriber();
            MockProducer producer2 = setupSubscriberAndValidate(sub2, expectedSubCount);
            assertThat("Unexpected items requested from producer.", producer2.getRequested(),
                       lessThanOrEqualTo(Math.max(1, defaultRequestN()/expectedSubCount)));
            sub2.onCompleted();
            sub2.unsubscribe();
            if (runPendingTasks) {
                channel.runPendingTasks();
            }
        }
    }
}
