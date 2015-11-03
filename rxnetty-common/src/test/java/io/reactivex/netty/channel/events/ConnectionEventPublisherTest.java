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
 *
 */
package io.reactivex.netty.channel.events;

import io.reactivex.netty.test.util.MockConnectionEventListener;
import io.reactivex.netty.test.util.MockConnectionEventListener.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ConnectionEventPublisherTest {

    @Rule
    public final PublisherRule rule = new PublisherRule();

    @Test(timeout = 60000)
    public void testOnByteRead() throws Exception {
        rule.publisher.onByteRead(1);
        rule.listener.assertMethodsCalled(Event.BytesRead);

        assertThat("Listener not called with bytes read.", rule.listener.getBytesRead(), is(1L));
    }

    @Test(timeout = 60000)
    public void testOnByteWritten() throws Exception {
        rule.publisher.onByteWritten(1);
        rule.listener.assertMethodsCalled(Event.BytesWritten);

        assertThat("Listener not called with bytes written.", rule.listener.getBytesWritten(), is(1L));
    }

    @Test(timeout = 60000)
    public void testOnFlushStart() throws Exception {
        rule.publisher.onFlushStart();
        rule.listener.assertMethodsCalled(Event.FlushStart);
    }

    @Test(timeout = 60000)
    public void testOnFlushSuccess() throws Exception {
        rule.publisher.onFlushComplete(1, TimeUnit.MILLISECONDS);
        rule.listener.assertMethodsCalled(Event.FlushSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnWriteStart() throws Exception {
        rule.publisher.onWriteStart();
        rule.listener.assertMethodsCalled(Event.WriteStart);
    }

    @Test(timeout = 60000)
    public void testOnWriteSuccess() throws Exception {
        rule.publisher.onWriteSuccess(1, TimeUnit.MILLISECONDS);
        rule.listener.assertMethodsCalled(Event.WriteSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnWriteFailed() throws Exception {
        final Throwable expected = new NullPointerException("Deliberate");
        rule.publisher.onWriteFailed(1, TimeUnit.MILLISECONDS, expected);
        rule.listener.assertMethodsCalled(Event.WriteFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseStart() throws Exception {
        rule.publisher.onConnectionCloseStart();
        rule.listener.assertMethodsCalled(Event.CloseStart);
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseSuccess() throws Exception {
        rule.publisher.onConnectionCloseSuccess(1, TimeUnit.MILLISECONDS);
        rule.listener.assertMethodsCalled(Event.CloseSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseFailed() throws Exception {
        final Throwable expected = new NullPointerException("Deliberate");
        rule.publisher.onConnectionCloseFailed(1, TimeUnit.MILLISECONDS, expected);
        rule.listener.assertMethodsCalled(Event.CloseFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testCustomEvent() throws Exception {
        Object event = "Hello";
        rule.publisher.onCustomEvent(event);
        rule.listener.assertMethodsCalled(Event.CustomEvent);

        assertThat("Listener not called with event.", rule.listener.getCustomEvent(), is(event));
    }

    @Test(timeout = 60000)
    public void testCustomEventWithError() throws Exception {
        final Throwable expected = new NullPointerException("Deliberate");
        Object event = "Hello";
        rule.publisher.onCustomEvent(event, expected);
        rule.listener.assertMethodsCalled(Event.CustomEventWithError);

        assertThat("Listener not called with event.", rule.listener.getCustomEvent(), is(event));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testCustomEventWithDuration() throws Exception {
        Object event = "Hello";
        rule.publisher.onCustomEvent(event, 1, TimeUnit.MILLISECONDS);
        rule.listener.assertMethodsCalled(Event.CustomEventWithDuration);

        assertThat("Listener not called with event.", rule.listener.getCustomEvent(), is(event));
        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testCustomEventWithDurationAndError() throws Exception {
        final Throwable expected = new NullPointerException("Deliberate");
        Object event = "Hello";
        rule.publisher.onCustomEvent(event, 1, TimeUnit.MILLISECONDS, expected);
        rule.listener.assertMethodsCalled(Event.CustomEventWithDurationAndError);

        assertThat("Listener not called with event.", rule.listener.getCustomEvent(), is(event));
        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testPublishingEnabled() throws Exception {
        assertThat("Publishing not enabled.", rule.publisher.publishingEnabled(), is(true));
    }

    @Test(timeout = 60000)
    public void testCopy() throws Exception {
        ConnectionEventPublisher<MockConnectionEventListener> copy = rule.publisher.copy();

        assertThat("Publisher not copied.", copy, is(not(sameInstance(rule.publisher))));
        assertThat("Listeners not copied.", copy.getListeners(), is(not(sameInstance(rule.publisher.getListeners()))));
    }

    public static class PublisherRule extends ExternalResource {

        private MockConnectionEventListener listener;
        private ConnectionEventPublisher<MockConnectionEventListener> publisher;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    listener = new MockConnectionEventListener();
                    publisher = new ConnectionEventPublisher<>();
                    publisher.subscribe(listener);
                    base.evaluate();
                }
            };
        }
    }

}