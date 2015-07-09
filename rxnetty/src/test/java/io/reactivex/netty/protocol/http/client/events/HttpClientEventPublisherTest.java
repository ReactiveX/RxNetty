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
package io.reactivex.netty.protocol.http.client.events;

import io.reactivex.netty.channel.events.ConnectionEventPublisherTest.ConnectionEventListenerImpl.Event;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListenerImpl.HttpEvent;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListenerImpl.ClientEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpClientEventPublisherTest {

    @Rule
    public final PublisherRule rule = new PublisherRule();

    @Test(timeout = 60000)
    public void testOnRequestSubmitted() throws Exception {
        rule.publisher.onRequestSubmitted();
        rule.listener.assertMethodCalled(HttpEvent.ReqSubmitted);
    }

    @Test(timeout = 60000)
    public void testOnRequestWriteStart() throws Exception {
        rule.publisher.onRequestWriteStart();
        rule.listener.assertMethodCalled(HttpEvent.ReqWriteStart);
    }

    @Test(timeout = 60000)
    public void testOnRequestWriteComplete() throws Exception {
        rule.publisher.onRequestWriteComplete(1, MILLISECONDS);
        rule.listener.assertMethodCalled(HttpEvent.ReqWriteSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnRequestWriteFailed() throws Exception {
        final Throwable expected = new NullPointerException();

        rule.publisher.onRequestWriteFailed(1, MILLISECONDS, expected);
        rule.listener.assertMethodCalled(HttpEvent.ReqWriteFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnResponseHeadersReceived() throws Exception {
        rule.publisher.onResponseHeadersReceived(200);
        rule.listener.assertMethodCalled(HttpEvent.ResHeadersReceived);

        assertThat("Listener not called with response code.", rule.listener.getResponseCode(), is(200));
    }

    @Test(timeout = 60000)
    public void testOnResponseContentReceived() throws Exception {
        rule.publisher.onResponseContentReceived();
        rule.listener.assertMethodCalled(HttpEvent.ResContentReceived);
    }

    @Test(timeout = 60000)
    public void testOnResponseReceiveComplete() throws Exception {
        rule.publisher.onResponseReceiveComplete(1, MILLISECONDS);
        rule.listener.assertMethodCalled(HttpEvent.ResReceiveComplete);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnResponseFailed() throws Exception {
        final Throwable expected = new NullPointerException();
        rule.publisher.onResponseFailed(expected);
        rule.listener.assertMethodCalled(HttpEvent.RespFailed);

        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnRequestProcessingComplete() throws Exception {
        rule.publisher.onRequestProcessingComplete(1, MILLISECONDS);
        rule.listener.assertMethodCalled(HttpEvent.ProcessingComplete);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseFailed() throws Exception {
        final Throwable expected = new NullPointerException();
        rule.publisher.onConnectionCloseFailed(1, MILLISECONDS, expected);

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.CloseFailed); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectStart() throws Exception {
        rule.publisher.onConnectStart();

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.ConnectStart); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectSuccess() throws Exception {
        rule.publisher.onConnectSuccess(1, MILLISECONDS);

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.ConnectSuccess); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectFailed() throws Exception {
        rule.publisher.onConnectFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.ConnectFailed); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPoolReleaseStart() throws Exception {
        rule.publisher.onPoolReleaseStart();

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.ReleaseStart); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPoolReleaseSuccess() throws Exception {
        rule.publisher.onPoolReleaseSuccess(1, MILLISECONDS);

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.ReleaseSuccess); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPoolReleaseFailed() throws Exception {
        rule.publisher.onPoolReleaseFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.ReleaseFailed); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPooledConnectionEviction() throws Exception {
        rule.publisher.onPooledConnectionEviction();

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.Eviction); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPooledConnectionReuse() throws Exception {
        rule.publisher.onPooledConnectionReuse();

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.Reuse); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPoolAcquireStart() throws Exception {
        rule.publisher.onPoolAcquireStart();

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.AcquireStart); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPoolAcquireSuccess() throws Exception {
        rule.publisher.onPoolAcquireSuccess(1, MILLISECONDS);

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.AcquireSuccess); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnPoolAcquireFailed() throws Exception {
        rule.publisher.onPoolAcquireFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().assertMethodsCalled(ClientEvent.AcquireFailed); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnByteRead() throws Exception {
        rule.publisher.onByteRead(1);

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.BytesRead); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushStart() throws Exception {
        rule.publisher.onFlushStart();

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.FlushStart); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushSuccess() throws Exception {
        rule.publisher.onFlushSuccess(1, MILLISECONDS);

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.FlushSuccess); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnFlushFailed() throws Exception {
        rule.publisher.onFlushFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.FlushFailed); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteStart() throws Exception {
        rule.publisher.onWriteStart();

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.WriteStart); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteSuccess() throws Exception {
        rule.publisher.onWriteSuccess(1, MILLISECONDS, 10);

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.WriteSuccess); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnWriteFailed() throws Exception {
        rule.publisher.onWriteFailed(1, MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.WriteFailed); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseStart() throws Exception {
        rule.publisher.onConnectionCloseStart();

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.CloseStart); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseSuccess() throws Exception {
        rule.publisher.onConnectionCloseSuccess(1, MILLISECONDS);

        rule.listener.getTcpDelegate().assertMethodsCalled(Event.CloseSuccess); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnCustomEvent() throws Exception {
        rule.publisher.onCustomEvent("Hello");
        rule.listener.getTcpDelegate().assertMethodsCalled(Event.CustomEvent); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnCustomEventWithError() throws Exception {
        rule.publisher.onCustomEvent("Hello", new NullPointerException());
        rule.listener.getTcpDelegate().assertMethodsCalled(Event.CustomEventWithError); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnCustomEventWithDuration() throws Exception {
        rule.publisher.onCustomEvent("Hello", 1, MINUTES);
        rule.listener.getTcpDelegate().assertMethodsCalled(Event.CustomEventWithDuration); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testOnCustomEventWithDurationAndError() throws Exception {
        rule.publisher.onCustomEvent("Hello", 1, MINUTES, new NullPointerException());
        rule.listener.getTcpDelegate().assertMethodsCalled(Event.CustomEventWithDurationAndError); // Test for TCP should verify rest
    }

    @Test(timeout = 60000)
    public void testPublishingEnabled() throws Exception {
        assertThat("Publishing not enabled.", rule.publisher.publishingEnabled(), is(true));
    }

    public static class PublisherRule extends ExternalResource {

        private HttpClientEventsListenerImpl listener;
        private HttpClientEventPublisher publisher;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    listener = new HttpClientEventsListenerImpl();
                    publisher = new HttpClientEventPublisher();
                    publisher.subscribe(listener);
                    base.evaluate();
                }
            };
        }
    }

}