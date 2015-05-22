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

import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.PrimitiveConversionHandler;
import io.reactivex.netty.protocol.tcp.BackpressureManagingHandler.BytesWriteInterceptor;
import io.reactivex.netty.protocol.tcp.BackpressureManagingHandler.WriteStreamSubscriber;
import io.reactivex.netty.test.util.MockProducer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class BytesWriteInterceptorTest {

    @Rule
    public final InspectorRule inspectorRule = new InspectorRule();

    @Test(timeout = 60000)
    public void testAddSubscriber() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        inspectorRule.interceptor.addSubscriber(sub1);

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        sub1.unsubscribe();

        assertThat("Subscriber not removed post unsubscribe", inspectorRule.interceptor.getSubscribers(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testRequestMore() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = InspectorRule.setupSubscriber(sub1);

        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(1L));
        mockProducer.reset();

        inspectorRule.interceptor.addSubscriber(sub1);

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        String msg = "Hello";
        inspectorRule.channel.writeAndFlush(msg);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested.", mockProducer.getRequested(), is(1L));
    }

    @Test(timeout = 60000)
    public void testRequestMorePostFlush() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = InspectorRule.setupSubscriber(sub1);

        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(1L));
        mockProducer.reset();

        inspectorRule.interceptor.addSubscriber(sub1);

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        inspectorRule.channel.config().setWriteBufferLowWaterMark(1);
        inspectorRule.channel.config().setWriteBufferHighWaterMark(2); /*Make sure that the channel is not writable on writing.*/

        String msg = "Hello";
        inspectorRule.channel.write(msg);

        assertThat("Channel still writable.", inspectorRule.channel.isWritable(), is(false));
        assertThat("More items requested when channel is not writable.", mockProducer.getRequested(), is(0L));

        inspectorRule.channel.flush();

        assertThat("Channel not writable post flush.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested.", mockProducer.getRequested(), is(1L));
    }

    @Test(timeout = 60000)
    public void testMultiSubscribers() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = InspectorRule.setupSubscriber(sub1);

        inspectorRule.interceptor.addSubscriber(sub1);
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        WriteStreamSubscriber sub2 = inspectorRule.newSubscriber();
        MockProducer producer2 = InspectorRule.setupSubscriber(sub2);

        inspectorRule.interceptor.addSubscriber(sub2);
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(2));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1, sub2));

        /*Reset before write*/
        producer1.reset();
        producer2.reset();

        String msg = "Hello";
        inspectorRule.channel.writeAndFlush(msg);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested from first subscriber.", producer1.getRequested(), is(1L));
        assertThat("Unexpected items requested from second subscriber.", producer2.getRequested(), is(1L));
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
                    channel = new EmbeddedChannel(PrimitiveConversionHandler.INSTANCE, interceptor);
                    base.evaluate();
                }
            };
        }

        WriteStreamSubscriber newSubscriber() {
            return new WriteStreamSubscriber(channel.pipeline().firstContext(), channel.newPromise());
        }

        private static MockProducer setupSubscriber(WriteStreamSubscriber sub1) {
            sub1.onStart();
            MockProducer mockProducer = new MockProducer();
            sub1.setProducer(mockProducer);
            return mockProducer;
        }
    }
}
