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

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Notification;
import rx.Notification.Kind;
import rx.Subscriber;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class AbstractConnectionToChannelBridgeTest {

    @Rule
    public final ConnectionHandlerRule connectionHandlerRule = new ConnectionHandlerRule();

    @Test
    public void testChannelActive() throws Exception {
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(false);

        connectionHandlerRule.activateConnectionAndAssert(subscriber);

        subscriber.notificationList.clear(); // reset

        connectionHandlerRule.handler.channelActive(connectionHandlerRule.ctx); // duplicate event should not trigger onNext.
        assertThat("Duplicate channel active event sent a notification", subscriber.notificationList, hasSize(0));
    }

    @Test
    public void testEagerContentSubscriptionFail() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(true); // should mandate eager content subscription
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(false);

        connectionHandlerRule.activateConnectionAndAssert(subscriber);

        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);

        assertThat("Unexpected notifications count after channel active.", inputSubscriber.notificationList,
                   hasSize(1));
        assertThat("Unexpected first notification kind.", inputSubscriber.notificationList.get(0).getKind(),
                   is(Kind.OnError));

    }

    @Test
    public void testEagerContentSubscriptionPass() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(true); // should mandate eager content subscription

        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);

        connectionHandlerRule.activateConnectionAndAssert(subscriber); // eagerly subscribes to input.
        ConnectionInputSubscriber inputSubscriber = subscriber.getInputSubscriber();

        assertThat("Unexpected notifications count after channel active.", inputSubscriber.notificationList,
                   hasSize(0));
        assertThat("Input subscriber is unsubscribed.", inputSubscriber.isUnsubscribed(),
                   is(false));
    }

    @Test
    public void testLazyContentSubscription() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(false); //lazy input sub.
        connectionHandlerRule.activateConnectionAndAssert(subscriber);
        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);

        assertThat("Unexpected notifications count after channel active.", inputSubscriber.notificationList,
                   hasSize(0));
        assertThat("Input subscriber is unsubscribed.", inputSubscriber.isUnsubscribed(),
                   is(false));

        connectionHandlerRule.testSendInputMsgs(inputSubscriber, "hello1");
    }

    @Test
    public void testInputCompleteOnChannelUnregister() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber);
        ConnectionInputSubscriber inputSubscriber = subscriber.getInputSubscriber(); // since sub is eager.

        connectionHandlerRule.testSendInputMsgs(inputSubscriber, "hello1");

        inputSubscriber.notificationList.clear();

        connectionHandlerRule.handler.channelUnregistered(connectionHandlerRule.ctx);
        assertThat("Unexpected notifications count after channel active.", inputSubscriber.notificationList,
                   hasSize(1));
        assertThat("Unexpected notifications count after channel active.", inputSubscriber.notificationList.get(0),
                   is(Notification.<String>createOnCompleted()));
    }

    @Test
    public void testCloseOnInputUnsubscribe() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber);
        Connection<String, String> connection = subscriber.notificationList.get(0).getValue();
        ConnectionInputSubscriber inputSubscriber = subscriber.getInputSubscriber(); // since sub is eager.

        connectionHandlerRule.testSendInputMsgs(inputSubscriber, "hello1");

        inputSubscriber.notificationList.clear();

        inputSubscriber.unsubscribe();

        assertThat("Connection not closed on unsubscribe.", connection.getNettyChannel().isOpen(), is(false));
    }

    @Test
    public void testMultipleInputSubscriptions() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber); // one subscription

        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);
        assertThat("Unexpected notifications for second subscriber.", inputSubscriber.notificationList, hasSize(1));
        assertThat("Unexpected notification type for second subscriber.",
                   inputSubscriber.notificationList.get(0).getKind(), is(Kind.OnError));
    }

    @Test
    public void testInputSubscriptionReset() throws Exception {
        connectionHandlerRule.channel.config().setAutoRead(false);
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber); // one subscription

        ConnectionInputSubscriber inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);
        assertThat("Unexpected notifications for second subscriber.", inputSubscriber.notificationList, hasSize(1));
        assertThat("Unexpected notification type for second subscriber.",
                   inputSubscriber.notificationList.get(0).getKind(), is(Kind.OnError));

        connectionHandlerRule.handler.userEventTriggered(connectionHandlerRule.ctx,
                                                         new ConnectionInputSubscriberResetEvent() {
                                                         });

        inputSubscriber = connectionHandlerRule.enableConnectionInputSubscriber(subscriber);
        assertThat("Unexpected notifications for input subscriber post reset.", inputSubscriber.notificationList,
                   hasSize(0));
    }

    @Test
    public void testErrorBeforeConnectionActive() throws Exception {
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        final NullPointerException exception = new NullPointerException();
        connectionHandlerRule.handler.exceptionCaught(connectionHandlerRule.ctx, exception);

        assertThat("Unexpected notifications count post exception.", subscriber.notificationList, hasSize(1));
        assertThat("Unexpected notification type post exception.", subscriber.notificationList.get(0).getKind(),
                   is(Kind.OnError));
    }

    @Test
    public void testErrorPostInputSubscribe() throws Exception {
        ConnectionSubscriber subscriber = connectionHandlerRule.enableConnectionSubscriberAndAssert(true);
        connectionHandlerRule.activateConnectionAndAssert(subscriber);
        ConnectionInputSubscriber inputSubscriber = subscriber.getInputSubscriber(); // since sub is eager.
        assertThat("Unexpected notifications count pre exception.", inputSubscriber.notificationList, hasSize(0));
        final NullPointerException exception = new NullPointerException();
        connectionHandlerRule.handler.exceptionCaught(connectionHandlerRule.ctx, exception);

        assertThat("Unexpected notifications count post exception.", inputSubscriber.notificationList, hasSize(1));
        assertThat("Unexpected notification type post exception.", inputSubscriber.notificationList.get(0).getKind(),
                   is(Kind.OnError));
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
                    handler = new AbstractConnectionToChannelBridge<String, String>(subject, provider) { };
                    base.evaluate();
                }
            };
        }

        public ConnectionSubscriber enableConnectionSubscriberAndAssert(boolean eagerSubToInput) throws Exception {
            ConnectionSubscriber toReturn = new ConnectionSubscriber(eagerSubToInput, this);
            handler.userEventTriggered(ctx, new ConnectionSubscriberEvent<String, String>(toReturn));
            assertThat("Unexpected notifications count before channel active.", toReturn.notificationList, hasSize(0));
            return toReturn;
        }

        public ConnectionInputSubscriber enableConnectionInputSubscriber(ConnectionSubscriber subscriber)
                throws Exception {
            ConnectionInputSubscriber toReturn = new ConnectionInputSubscriber();
            Connection<String, String> connection = subscriber.notificationList.get(0).getValue();
            handler.userEventTriggered(ctx, new ConnectionInputSubscriberEvent<String, String>(toReturn, connection));
            return toReturn;
        }

        public void activateConnectionAndAssert(ConnectionSubscriber subscriber) throws Exception {
            handler.channelActive(ctx);

            assertThat("Unexpected notifications count after channel active.", subscriber.notificationList,
                       hasSize(2));
            Notification<Connection<String, String>> connNotif = subscriber.notificationList.get(0);
            assertThat("Unexpected first notification kind.", connNotif.getKind(), is(Kind.OnNext));
            assertThat("Unexpected onNext.", connNotif.getValue(), is(notNullValue()));
            assertThat("Unexpected channel in new connection.", connNotif.getValue().getNettyChannel(), is(channel));
            assertThat("Unexpected second notification.", subscriber.notificationList.get(1).getKind(),
                       is(Kind.OnCompleted));
        }

        public void testSendInputMsgs(ConnectionInputSubscriber inputSubscriber, String... msgs) throws Exception {
            for (String msg: msgs) {
                handler.channelRead(ctx, msg);
                assertThat("Unexpected notifications count after read.", inputSubscriber.notificationList,
                           hasSize(1));
                assertThat("Input subscriber is unsubscribed after read.", inputSubscriber.isUnsubscribed(),
                           is(false));
                assertThat("Unexpected next input notification.", inputSubscriber.notificationList.get(0).getValue(),
                           is(msg));
            }
        }
    }

    public static class ConnectionSubscriber extends TestableSubscriber<Connection<String, String>> {

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

    public static class ConnectionInputSubscriber extends TestableSubscriber<String> {

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

        public void requestMore(long more) {
            request(more);
        }
    }

    public static class TestableSubscriber<T> extends Subscriber<T> {

        protected final List<Notification<T>> notificationList = new ArrayList<>();

        @Override
        public void onCompleted() {
            notificationList.add(Notification.<T>createOnCompleted());
        }

        @Override
        public void onError(Throwable e) {
            notificationList.add(Notification.<T>createOnError(e));
        }

        @Override
        public void onNext(T connection) {
            notificationList.add(Notification.createOnNext(connection));
        }
    }
}