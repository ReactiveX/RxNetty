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
package io.reactivex.netty.protocol.tcp.server.events;

import io.reactivex.netty.channel.events.ConnectionEventPublisherTest.ConnectionEventListenerImpl.Event;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventListenerImpl.ServerEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TcpServerEventPublisherTest {

    @Rule
    public final PublisherRule rule = new PublisherRule();

    @Test(timeout = 60000)
    public void testOnNewClientConnected() throws Exception {
        rule.publisher.onNewClientConnected();

        rule.listener.assertMethodsCalled(ServerEvent.NewClient);
    }

    @Test(timeout = 60000)
    public void testOnConnectionHandlingStart() throws Exception {
        rule.publisher.onConnectionHandlingStart(1, MILLISECONDS);

        rule.listener.assertMethodsCalled(ServerEvent.HandlingStart);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnConnectionHandlingSuccess() throws Exception {
        rule.publisher.onConnectionHandlingSuccess(1, MILLISECONDS);

        rule.listener.assertMethodsCalled(ServerEvent.HandlingSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnConnectionHandlingFailed() throws Exception {
        final Throwable expected = new NullPointerException();

        rule.publisher.onConnectionHandlingFailed(1, MILLISECONDS, expected);

        rule.listener.assertMethodsCalled(ServerEvent.HandlingFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnByteRead() throws Exception {
        rule.publisher.onByteRead(1);

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.BytesRead); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushStart() throws Exception {
        rule.publisher.onFlushStart();

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.FlushStart); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushSuccess() throws Exception {
        rule.publisher.onFlushSuccess(1, MILLISECONDS);

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.FlushSuccess); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushFailed() throws Exception {
        rule.publisher.onFlushFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.FlushFailed); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteStart() throws Exception {
        rule.publisher.onWriteStart();

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.WriteStart); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteSuccess() throws Exception {
        rule.publisher.onWriteSuccess(1, MILLISECONDS, 10);

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.WriteSuccess); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteFailed() throws Exception {
        rule.publisher.onWriteFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.WriteFailed); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseStart() throws Exception {
        rule.publisher.onConnectionCloseStart();

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.CloseStart); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseSuccess() throws Exception {
        rule.publisher.onConnectionCloseSuccess(1, MILLISECONDS);

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.CloseSuccess); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseFailed() throws Exception {
        rule.publisher.onConnectionCloseFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getConnDelegate().assertMethodsCalledAfterSubscription(Event.CloseFailed); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testPublishingEnabled() throws Exception {
        assertThat("Publishing not enabled.", rule.publisher.publishingEnabled(), is(true));
    }

    @Test(timeout = 60000)
    public void testSubscribe() throws Exception {
        rule.listener.getConnDelegate().assertMethodsCalled(Event.Subscribe);
    }

    @Test(timeout = 60000)
    public void testCopy() throws Exception {
        final TcpServerEventPublisher copy = rule.publisher.copy();

        assertThat("Publisher not copied.", copy, is(not(sameInstance(rule.publisher))));
        assertThat("Listeners not copied.", copy.getListeners(), is(not(sameInstance(rule.publisher.getListeners()))));
        assertThat("Listeners not copied.", copy.getConnDelegate(),
                   is(not(sameInstance(rule.publisher.getConnDelegate()))));
    }

    public static class PublisherRule extends ExternalResource {

        private TcpServerEventListenerImpl listener;
        private TcpServerEventPublisher publisher;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    listener = new TcpServerEventListenerImpl();
                    publisher = new TcpServerEventPublisher();
                    publisher.subscribe(listener);
                    base.evaluate();
                }
            };
        }
    }
}