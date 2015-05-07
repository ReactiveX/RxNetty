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
package io.reactivex.netty.protocol.http.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge.ConnectionInputSubscriber;
import io.reactivex.netty.protocol.tcp.ConnectionInputSubscriberEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Matchers;
import org.mockito.Mockito;
import rx.Producer;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class AbstractHttpConnectionBridgeTest {

    @Rule
    public final HandlerRule handlerRule = new HandlerRule();

    @Test
    public void testWritePrimitives() throws Exception {
        handlerRule.channel.writeAndFlush("Hello");

        assertThat("Unexpected outbound message count.", handlerRule.channel.outboundMessages(), hasSize(1));
        assertThat("Unexpected message written.", handlerRule.channel.readOutbound(), instanceOf(ByteBuf.class));

        handlerRule.channel.writeAndFlush("Hello".getBytes());

        assertThat("Unexpected outbound message count.", handlerRule.channel.outboundMessages(), hasSize(1));
        assertThat("Unexpected message written.", handlerRule.channel.readOutbound(), instanceOf(ByteBuf.class));
    }

    @Test
    public void testConnInputSubscriberEvent() throws Exception {
        handlerRule.setupAndAssertConnectionInputSub();
    }

    @Test
    public void testHttpContentSubscriberEventWithNoContentInputSub() throws Exception {
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        handlerRule.channel.pipeline().fireUserEventTriggered(new HttpContentSubscriberEvent<String>(subscriber));

        subscriber.assertTerminalEvent();

        assertThat("Subscriber did not get an error", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Subscriber got an unexpected error", subscriber.getOnErrorEvents().get(0),
                   instanceOf(IllegalStateException.class));
    }

    @Test
    public void testHttpContentSub() throws Exception {
        handlerRule.setupAndAssertConnectionInputSub();

        ProducerAwareSubscriber subscriber = new ProducerAwareSubscriber();
        handlerRule.channel.pipeline().fireUserEventTriggered(new HttpContentSubscriberEvent<String>(subscriber));

        subscriber.assertNoErrors();

        @SuppressWarnings("unchecked")
        Subscriber<String> contentSub = (Subscriber<String>) handlerRule.connInSub.getState().getContentSub();

        assertThat("Unexpected HTTP Content subscriber found", contentSub, equalTo((Subscriber<String>)subscriber));
        assertThat("Header subscriber producer not set.", subscriber.getProducer(),
                   equalTo(handlerRule.connInputProducerMock));

        subscriber.unsubscribe();

        subscriber.assertUnsubscribed();

        assertThat("Unsubscribing from HTTP content, did not unsubscribe from connection input.",
                   handlerRule.connInSub.isUnsubscribed(), is(true));
    }

    public static class HandlerRule extends ExternalResource {

        private Connection<String, String> connMock;
        private EmbeddedChannel channel;
        private AbstractHttpConnectionBridgeMock handler;
        private EventCatcher eventCatcher;
        private ConnectionInputSubscriber connInSub;
        private Producer connInputProducerMock;
        private ProducerAwareSubscriber headerSub;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    handler = new AbstractHttpConnectionBridgeMock();
                    eventCatcher = new EventCatcher();
                    channel = new EmbeddedChannel(handler, eventCatcher);
                    @SuppressWarnings("unchecked")
                    Connection<String, String> connMock = Mockito.mock(Connection.class);
                    Mockito.when(connMock.getNettyChannel()).thenReturn(channel);

                    HandlerRule.this.connMock = connMock;
                    base.evaluate();
                }
            };
        }

        protected void setupAndAssertConnectionInputSub() {
            headerSub = new ProducerAwareSubscriber();

            @SuppressWarnings({"rawtypes", "unchecked"})
            ConnectionInputSubscriberEvent evt = new ConnectionInputSubscriberEvent(headerSub, connMock);

            channel.pipeline().fireUserEventTriggered(evt);

            assertThat("Handler did not pass the event.", eventCatcher.events, hasSize(1));
            assertThat("Handler did not modify the event.", eventCatcher.events, not(contains((Object) evt)));

            Object eventCaught = eventCatcher.events.get(0);

            assertThat("Unexpected propagated event.", eventCaught, instanceOf(ConnectionInputSubscriberEvent.class));

            @SuppressWarnings({"rawtypes", "unchecked"})
            ConnectionInputSubscriberEvent modEvt = (ConnectionInputSubscriberEvent) eventCaught;

            assertThat("Unexpected propagated event subscriber.", modEvt.getSubscriber(),
                       instanceOf(ConnectionInputSubscriber.class));

            @SuppressWarnings("unchecked")
            ConnectionInputSubscriber connInSub = (ConnectionInputSubscriber) modEvt.getSubscriber();
            this.connInSub = connInSub;

            assertThat("Channel not set in the subscriber.", connInSub.getChannel(), is(notNullValue()));
            assertThat("Unexpected channel set in the subscriber.", connInSub.getChannel(), equalTo((Channel)channel));

            @SuppressWarnings("unchecked")
            Subscriber<String> headerSub = (Subscriber<String>) connInSub.getState().getHeaderSub();

            assertThat("Unexpected header subscriber.", headerSub, is((Subscriber<String>) this.headerSub));

            connInputProducerMock = Mockito.mock(Producer.class);
            connInSub.setProducer(connInputProducerMock);

            assertThat("Header subscriber producer not set.", this.headerSub.getProducer(),
                       equalTo(connInputProducerMock));

            Mockito.verify(connInputProducerMock).request(Matchers.anyLong());
        }

    }

    private static class ProducerAwareSubscriber extends TestSubscriber<String> {

        private Producer producer;

        @Override
        public void setProducer(Producer producer) {
            this.producer = producer;
            super.setProducer(producer);
        }

        public Producer getProducer() {
            return producer;
        }
    }

    private static class EventCatcher extends ChannelDuplexHandler {

        private final List<Object> events = new ArrayList<>();

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            events.add(evt);
            super.userEventTriggered(ctx, evt);
        }
    }

    public static class AbstractHttpConnectionBridgeMock extends AbstractHttpConnectionBridge<String> {

        @Override
        protected boolean isHeaderMessage(Object nextItem) {
            //TODO: Auto-generated
            return false;
        }

        @Override
        protected Object newHttpObject(Object nextItem, Channel channel) {
            //TODO: Auto-generated
            return null;
        }

        @Override
        protected void onContentReceived() {
            //TODO: Auto-generated

        }

        @Override
        protected void onContentReceiveComplete(long receiveStartTimeMillis) {
            //TODO: Auto-generated

        }
    }
}
