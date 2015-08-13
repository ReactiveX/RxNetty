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
package io.reactivex.netty.protocol.tcp.client.events;

import io.reactivex.netty.protocol.tcp.client.MockTcpClientEventListener;
import io.reactivex.netty.test.util.MockClientEventListener.ClientEvent;
import io.reactivex.netty.test.util.MockConnectionEventListener.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TcpClientEventPublisherTest {

    @Rule
    public PublisherRule rule = new PublisherRule();

    @Test(timeout = 60000)
    public void testOnConnectStart() throws Exception {
        rule.publisher.onConnectStart();

        rule.listener.assertMethodsCalled(ClientEvent.ConnectStart);
    }

    @Test(timeout = 60000)
    public void testOnConnectSuccess() throws Exception {
        rule.publisher.onConnectSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.assertMethodsCalled(ClientEvent.ConnectSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnConnectFailed() throws Exception {
        final Throwable expected = new NullPointerException();

        rule.publisher.onConnectFailed(1, TimeUnit.MILLISECONDS, expected);

        rule.listener.assertMethodsCalled(ClientEvent.ConnectFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnPoolReleaseStart() throws Exception {
        rule.publisher.onPoolReleaseStart();

        rule.listener.assertMethodsCalled(ClientEvent.ReleaseStart);
    }

    @Test(timeout = 60000)
    public void testOnPoolReleaseSuccess() throws Exception {
        rule.publisher.onPoolReleaseSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.assertMethodsCalled(ClientEvent.ReleaseSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnPoolReleaseFailed() throws Exception {
        final Throwable expected = new NullPointerException();

        rule.publisher.onPoolReleaseFailed(1, TimeUnit.MILLISECONDS, expected);

        rule.listener.assertMethodsCalled(ClientEvent.ReleaseFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnPooledConnectionEviction() throws Exception {
        rule.publisher.onPooledConnectionEviction();

        rule.listener.assertMethodsCalled(ClientEvent.Eviction);
    }

    @Test(timeout = 60000)
    public void testOnPooledConnectionReuse() throws Exception {
        rule.publisher.onPooledConnectionReuse();

        rule.listener.assertMethodsCalled(ClientEvent.Reuse);
    }

    @Test(timeout = 60000)
    public void testOnPoolAcquireStart() throws Exception {
        rule.publisher.onPoolAcquireStart();

        rule.listener.assertMethodsCalled(ClientEvent.AcquireStart);
    }

    @Test(timeout = 60000)
    public void testOnPoolAcquireSuccess() throws Exception {
        rule.publisher.onPoolAcquireSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.assertMethodsCalled(ClientEvent.AcquireSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnPoolAcquireFailed() throws Exception {
        final Throwable expected = new NullPointerException();

        rule.publisher.onPoolAcquireFailed(1, TimeUnit.MILLISECONDS, expected);

        rule.listener.assertMethodsCalled(ClientEvent.AcquireFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnByteRead() throws Exception {
        rule.publisher.onByteRead(1);

        rule.listener.assertMethodsCalled(Event.BytesRead); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushStart() throws Exception {
        rule.publisher.onFlushStart();

        rule.listener.assertMethodsCalled(Event.FlushStart); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushSuccess() throws Exception {
        rule.publisher.onFlushSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.assertMethodsCalled(Event.FlushSuccess); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushFailed() throws Exception {
        rule.publisher.onFlushFailed(1, TimeUnit.MILLISECONDS, new NullPointerException());

        rule.listener.assertMethodsCalled(Event.FlushFailed); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteStart() throws Exception {
        rule.publisher.onWriteStart();

        rule.listener.assertMethodsCalled(Event.WriteStart); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteSuccess() throws Exception {
        rule.publisher.onWriteSuccess(1, TimeUnit.MILLISECONDS, 10);

        rule.listener.assertMethodsCalled(Event.WriteSuccess); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteFailed() throws Exception {
        rule.publisher.onWriteFailed(1, TimeUnit.MILLISECONDS, new NullPointerException());

        rule.listener.assertMethodsCalled(Event.WriteFailed); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseStart() throws Exception {
        rule.publisher.onConnectionCloseStart();

        rule.listener.assertMethodsCalled(Event.CloseStart); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseSuccess() throws Exception {
        rule.publisher.onConnectionCloseSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.assertMethodsCalled(Event.CloseSuccess); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseFailed() throws Exception {
        rule.publisher.onConnectionCloseFailed(1, TimeUnit.MILLISECONDS, new NullPointerException());

        rule.listener.assertMethodsCalled(Event.CloseFailed); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testPublishingEnabled() throws Exception {
        assertThat("Publishing not enabled.", rule.publisher.publishingEnabled(), is(true));
    }

    @Test(timeout = 60000)
    public void testCustomEvent() throws Exception {
        rule.publisher.onCustomEvent("Hello");
        rule.listener.assertMethodsCalled(Event.CustomEvent); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testCustomEventWithError() throws Exception {
        rule.publisher.onCustomEvent("Hello", new NullPointerException());
        rule.listener.assertMethodsCalled(Event.CustomEventWithError); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testCustomEventWithDuration() throws Exception {
        rule.publisher.onCustomEvent("Hello", 1, TimeUnit.MINUTES);
        rule.listener.assertMethodsCalled(Event.CustomEventWithDuration); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testCustomEventWithDurationAndError() throws Exception {
        rule.publisher.onCustomEvent("Hello", 1, TimeUnit.MINUTES, new NullPointerException());
        rule.listener.assertMethodsCalled(Event.CustomEventWithDurationAndError); // Test for Connection publisher should verify rest
    }

    @Test(timeout = 60000)
    public void testCopy() throws Exception {
        TcpClientEventPublisher copy = rule.publisher.copy();

        assertThat("Publisher not copied.", copy, is(not(sameInstance(rule.publisher))));
        assertThat("Listeners not copied.", copy.getListeners(), is(not(sameInstance(rule.publisher.getListeners()))));
    }

    public static class PublisherRule extends ExternalResource {

        private MockTcpClientEventListener listener;
        private TcpClientEventPublisher publisher;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    listener = new MockTcpClientEventListener();
                    publisher = new TcpClientEventPublisher();
                    publisher.subscribe(listener);
                    base.evaluate();
                }
            };
        }
    }
}