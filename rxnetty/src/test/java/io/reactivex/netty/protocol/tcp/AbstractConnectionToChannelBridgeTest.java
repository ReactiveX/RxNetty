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

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.tcp.BackpressureManagingHandler.RequestReadIfRequiredEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.observers.TestSubscriber;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class AbstractConnectionToChannelBridgeTest {

    @Rule
    public final ConnectionHandlerRule connectionHandlerRule = new ConnectionHandlerRule();

    @Test
    public void testChannelActive() throws Exception {
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(false);

        connectionHandlerRule.activateConnectionAndAssert(subscriber);

        assertThat("Duplicate channel active event sent a notification", subscriber.getOnNextEvents(), hasSize(1));
        connectionHandlerRule.handler.channelActive(connectionHandlerRule.ctx); // duplicate event should not trigger onNext.
        /*One item from activation*/
        assertThat("Duplicate channel active event sent a notification", subscriber.getOnNextEvents(), hasSize(1));
    }

    @Test
    public void testEagerContentSubscriptionFail() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(true); // should mandate eager content subscription
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(false);

        connectionHandlerRule.activateConnectionAndAssert(subscriber);

        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);

        subscriber.assertTerminalEvent();
        assertThat("Unexpected first notification kind.", inputSubscriber.getOnErrorEvents(), hasSize(1));

    }

    @Test
    public void testEagerContentSubscriptionPass() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(true); // should mandate eager content subscription

        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);

        connectionHandlerRule.activateConnectionAndAssert(subscriber); // eagerly subscribes to input.
        ConnectionInputSubscriber inputSubscriber = subscriber.getInputSubscriber();

        assertThat("Unexpected notifications count after channel active.", inputSubscriber.getOnNextEvents(),
                   hasSize(0));
        inputSubscriber.assertNoErrors();
        assertThat("Input subscriber is unsubscribed.", inputSubscriber.isUnsubscribed(), is(false));
    }

    @Test
    public void testLazyContentSubscription() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(false); //lazy input sub.
        connectionHandlerRule.activateConnectionAndAssert(subscriber);
        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);

        inputSubscriber.assertNoErrors();
        assertThat("Unexpected on next events after channel active.", inputSubscriber.getOnNextEvents(),
                   hasSize(0));
        assertThat("Unexpected on completed events after channel active.", inputSubscriber.getOnCompletedEvents(),
                   hasSize(0));
        assertThat("Input subscriber is unsubscribed.", inputSubscriber.isUnsubscribed(), is(false));

        connectionHandlerRule.startRead();
        connectionHandlerRule.testSendInputMsgs(inputSubscriber, "hello1");
    }

    @Test
    public void testInputCompleteOnChannelUnregister() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber);
        ConnectionInputSubscriber inputSubscriber = subscriber.getInputSubscriber(); // since sub is eager.
        connectionHandlerRule.startRead();
        connectionHandlerRule.testSendInputMsgs(inputSubscriber, "hello1");


        assertThat("Unexpected notifications count after channel active.", inputSubscriber.getOnNextEvents(),
                   hasSize(1));
        inputSubscriber.unsubscribe(); // else channel close will generate error if subscribed
        connectionHandlerRule.handler.channelUnregistered(connectionHandlerRule.ctx);
        inputSubscriber.assertNoErrors();
        assertThat("Unexpected notifications count after channel active.", inputSubscriber.getOnNextEvents(),
                   hasSize(1));
    }

    @Test
    public void testMultipleInputSubscriptions() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber); // one subscription

        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);

        inputSubscriber.assertTerminalEvent();

        assertThat("Unexpected on next events for second subscriber.", inputSubscriber.getOnNextEvents(), hasSize(0));
        assertThat("Unexpected notification type for second subscriber.", inputSubscriber.getOnErrorEvents(),
                   hasSize(1));
    }

    @Test
    public void testInputSubscriptionReset() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber); // one subscription

        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);
        inputSubscriber.assertTerminalEvent();
        assertThat("Unexpected on next events for second subscriber.", inputSubscriber.getOnNextEvents(), hasSize(0));

        connectionHandlerRule.handler.userEventTriggered(connectionHandlerRule.ctx,
                                                         new ConnectionInputSubscriberResetEvent() {
                                                         });

        inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);
        assertThat("Unexpected on next count for input subscriber post reset.", inputSubscriber.getOnNextEvents(),
                   hasSize(0));
        assertThat("Unexpected on error count for input subscriber post reset.", inputSubscriber.getOnErrorEvents(),
                   hasSize(0));
        assertThat("Unexpected on completed count for input subscriber post reset.",
                   inputSubscriber.getOnCompletedEvents(), hasSize(0));
    }

    @Test
    public void testErrorBeforeConnectionActive() throws Exception {
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        final NullPointerException exception = new NullPointerException();
        connectionHandlerRule.handler.exceptionCaught(connectionHandlerRule.ctx, exception);

        subscriber.assertTerminalEvent();

        assertThat("Unexpected on next notifications count post exception.", subscriber.getOnNextEvents(), hasSize(0));
        assertThat("Unexpected notification type post exception.", subscriber.getOnErrorEvents(), hasSize(1));
    }

    @Test
    public void testErrorPostInputSubscribe() throws Exception {
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber);
        ConnectionInputSubscriber inputSubscriber = subscriber.getInputSubscriber(); // since sub is eager.

        assertThat("Unexpected on next notifications count pre exception.", inputSubscriber.getOnNextEvents(), hasSize(0));
        assertThat("Unexpected on error notifications count pre exception.", inputSubscriber.getOnErrorEvents(), hasSize(0));
        assertThat("Unexpected on completed notifications count pre exception.", inputSubscriber.getOnCompletedEvents(), hasSize(0));
        final NullPointerException exception = new NullPointerException();
        connectionHandlerRule.handler.exceptionCaught(connectionHandlerRule.ctx, exception);

        inputSubscriber.assertTerminalEvent();

        assertThat("Unexpected on next notifications count post exception.", inputSubscriber.getOnNextEvents(), hasSize(0));
        assertThat("Unexpected on error notifications count post exception.", inputSubscriber.getOnErrorEvents(),
                   hasSize(1));
    }

    public static class ConnectionHandlerRule extends ExternalResource {

        private Channel channel;
        private ChannelHandlerContext ctx;
        private AbstractConnectionToChannelBridge<String, String> handler;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    channel = new EmbeddedChannel(new ChannelDuplexHandler());
                    ctx = channel.pipeline().firstContext();
                    MetricEventsSubject<ClientMetricsEvent<?>> subject = new MetricEventsSubject<>();
                    ClientChannelMetricEventProvider provider = ClientChannelMetricEventProvider.INSTANCE;
                    handler = new AbstractConnectionToChannelBridge<String, String>("foo", subject, provider) { };
                    base.evaluate();
                }
            };
        }

        public void startRead() throws Exception {
            handler.userEventTriggered(ctx, new RequestReadIfRequiredEvent() {
                @Override
                protected boolean shouldReadMore(ChannelHandlerContext ctx) {
                    return true;
                }
            });
        }

        public ConnectionSubscriber enableConnectionSubscriberAndAssert(boolean eagerSubToInput) throws Exception {
            ConnectionSubscriber toReturn = new ConnectionSubscriber(eagerSubToInput, this);
            handler.userEventTriggered(ctx, new ConnectionSubscriberEvent<String, String>(toReturn));
            assertThat("Unexpected on next notifications count before channel active.", toReturn.getOnNextEvents(),
                       hasSize(0));
            assertThat("Unexpected on error notifications count before channel active.", toReturn.getOnErrorEvents(),
                       hasSize(0));
            assertThat("Unexpected on complete notifications count before channel active.", toReturn.getOnCompletedEvents(), hasSize(0));
            return toReturn;
        }

        public ConnectionInputSubscriber enableConnectionInputSubscriber(ConnectionSubscriber subscriber)
                throws Exception {
            ConnectionInputSubscriber toReturn = new ConnectionInputSubscriber();
            Connection<String, String> connection = subscriber.getOnNextEvents().get(0);
            handler.userEventTriggered(ctx, new ConnectionInputSubscriberEvent<String, String>(toReturn, connection));
            return toReturn;
        }

        public void activateConnectionAndAssert(ConnectionSubscriber subscriber) throws Exception {
            handler.userEventTriggered(ctx, EmitConnectionEvent.INSTANCE);

            subscriber.assertTerminalEvent();
            subscriber.assertNoErrors();

            assertThat("No connections received.", subscriber.getOnNextEvents(), is(not(empty())));
            assertThat("Unexpected channel in new connection.", subscriber.getOnNextEvents().get(0).unsafeNettyChannel(),
                       is(channel));

        }

        public void testSendInputMsgs(ConnectionInputSubscriber inputSubscriber, String... msgs) throws Exception {

            for (String msg: msgs) {
                handler.channelRead(ctx, msg);
            }

            assertThat("Unexpected notifications count after read.", inputSubscriber.getOnNextEvents(),
                       hasSize(msgs.length));
            assertThat("Unexpected notifications count after read.", inputSubscriber.getOnNextEvents(),
                       contains(msgs));

            assertThat("Input subscriber is unsubscribed after read.", inputSubscriber.isUnsubscribed(),
                       is(false));
        }
    }

    public static class ConnectionSubscriber extends TestSubscriber<Connection<String, String>> {

        private final boolean subscribeToInput;
        private final ConnectionHandlerRule rule;
        private ConnectionInputSubscriber inputSubscriber;

        public ConnectionSubscriber(boolean subscribeToInput, ConnectionHandlerRule rule) {
            this.subscribeToInput = subscribeToInput;
            this.rule = rule;
        }

        @Override
        public void onNext(Connection<String, String> connection) {
            super.onNext(connection);
            try {
                if (subscribeToInput) {
                    inputSubscriber = rule.enableConnectionInputSubscriber(this);
                }
            } catch (Exception e) {
                onError(e);
            }
        }

        public ConnectionInputSubscriber getInputSubscriber() {
            return inputSubscriber;
        }
    }

    public static class ConnectionInputSubscriber extends TestSubscriber<String> {

        private final long requestAtStart;

        public ConnectionInputSubscriber() {
            this(Long.MAX_VALUE);
        }

        public ConnectionInputSubscriber(long requestAtStart) {
            this.requestAtStart = requestAtStart;
        }

        @Override
        public void onStart() {
            request(requestAtStart);
        }
    }
}