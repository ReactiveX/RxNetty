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
import org.hamcrest.Matcher;
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

        assertThat("Subscriber not removed post unsubscribe", inspectorRule.interceptor.getSubscribers(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testRequestMore() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = InspectorRule.setupSubscriber(sub1);

        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(defaultRequestN()));

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        String msg = "Hello";
        inspectorRule.channel.writeAndFlush(msg);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested.", mockProducer.getRequested(), is(defaultRequestN()));
    }

    @Test(timeout = 60000)
    public void testRequestMorePostFlush() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = InspectorRule.setupSubscriber(sub1);

        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(defaultRequestN()));

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

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
        MockProducer producer1 = InspectorRule.setupSubscriber(sub1);

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        WriteStreamSubscriber sub2 = inspectorRule.newSubscriber();
        MockProducer producer2 = InspectorRule.setupSubscriber(sub2);

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(2));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1, sub2));

        String msg = "Hello";
        inspectorRule.channel.writeAndFlush(msg);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested from first subscriber.", producer1.getRequested(),
                   is(defaultRequestN()));
        assertThat("Unexpected items requested from second subscriber.", producer2.getRequested(),
                   is(defaultRequestN() / 2));
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
            return interceptor.newSubscriber(channel.pipeline().firstContext(), channel.newPromise());
        }

        private static MockProducer setupSubscriber(WriteStreamSubscriber sub1) {
            sub1.onStart();
            MockProducer mockProducer = new MockProducer();
            sub1.setProducer(mockProducer);
            return mockProducer;
        }

        public static Long defaultRequestN() {
            return Long.valueOf(MAX_PER_SUBSCRIBER_REQUEST);
        }
    }
}
