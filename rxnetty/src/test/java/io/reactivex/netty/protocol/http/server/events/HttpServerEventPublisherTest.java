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
package io.reactivex.netty.protocol.http.server.events;

import io.reactivex.netty.channel.events.ConnectionEventPublisherTest.ConnectionEventListenerImpl.Event;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventsListenerImpl.HttpEvent;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventListenerImpl.ServerEvent;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpServerEventPublisherTest {

    @Rule
    public final PublisherRule rule = new PublisherRule();

    @Test(timeout = 60000)
    public void testOnNewRequestReceived() throws Exception {
        rule.publisher.onNewRequestReceived();

        rule.listener.assertMethodsCalled(HttpEvent.ReqRecv);
    }

    @Test(timeout = 60000)
    public void testOnRequestHandlingStart() throws Exception {
        rule.publisher.onRequestHandlingStart(1, TimeUnit.MILLISECONDS);

        rule.listener.assertMethodsCalled(HttpEvent.HandlingStart);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnRequestHandlingSuccess() throws Exception {
        rule.publisher.onRequestHandlingSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.assertMethodsCalled(HttpEvent.HandlingSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnRequestHandlingFailed() throws Exception {
        final Throwable expected = new NullPointerException();

        rule.publisher.onRequestHandlingFailed(1, TimeUnit.MILLISECONDS, expected);

        rule.listener.assertMethodsCalled(HttpEvent.HandlingFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnRequestHeadersReceived() throws Exception {
        rule.publisher.onRequestHeadersReceived();
        rule.listener.assertMethodsCalled(HttpEvent.ReqHdrsReceived);
    }

    @Test(timeout = 60000)
    public void testOnRequestContentReceived() throws Exception {
        rule.publisher.onRequestContentReceived();
        rule.listener.assertMethodsCalled(HttpEvent.ReqContentReceived);
    }

    @Test(timeout = 60000)
    public void testOnRequestReceiveComplete() throws Exception {
        rule.publisher.onRequestReceiveComplete(1, TimeUnit.MILLISECONDS);
        rule.listener.assertMethodsCalled(HttpEvent.ReqReceiveComplete);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnResponseWriteStart() throws Exception {
        rule.publisher.onResponseWriteStart();
        rule.listener.assertMethodsCalled(HttpEvent.RespWriteStart);
    }

    @Test(timeout = 60000)
    public void testOnResponseWriteSuccess() throws Exception {
        rule.publisher.onResponseWriteSuccess(1, TimeUnit.MILLISECONDS, 200);
        rule.listener.assertMethodsCalled(HttpEvent.RespWriteSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with response code.", rule.listener.getResponseCode(), is(200));
    }

    @Test(timeout = 60000)
    public void testOnResponseWriteFailed() throws Exception {
        final Throwable expected = new NullPointerException();

        rule.publisher.onResponseWriteFailed(1, TimeUnit.MILLISECONDS, expected);

        rule.listener.assertMethodsCalled(HttpEvent.RespWriteFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseFailed() throws Exception {
        rule.publisher.onConnectionCloseFailed(1, TimeUnit.MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.CloseFailed);
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseSuccess() throws Exception {
        rule.publisher.onConnectionCloseSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.CloseSuccess);
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseStart() throws Exception {
        rule.publisher.onConnectionCloseStart();

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.CloseStart);
    }

    @Test(timeout = 60000)
    public void testOnWriteFailed() throws Exception {
        rule.publisher.onWriteFailed(1, TimeUnit.MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.WriteFailed);
    }

    @Test(timeout = 60000)
    public void testOnWriteSuccess() throws Exception {
        rule.publisher.onWriteSuccess(1, TimeUnit.MILLISECONDS, 10);

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.WriteSuccess);
    }

    @Test(timeout = 60000)
    public void testOnWriteStart() throws Exception {
        rule.publisher.onWriteStart();

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.WriteStart);
    }

    @Test(timeout = 60000)
    public void testOnFlushFailed() throws Exception {
        rule.publisher.onFlushFailed(1, TimeUnit.MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.FlushFailed);
    }

    @Test(timeout = 60000)
    public void testOnFlushSuccess() throws Exception {
        rule.publisher.onFlushSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.FlushSuccess);
    }

    @Test(timeout = 60000)
    public void testOnFlushStart() throws Exception {
        rule.publisher.onFlushStart();

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.FlushStart);
    }

    @Test(timeout = 60000)
    public void testOnByteRead() throws Exception {
        rule.publisher.onByteRead(1);

        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalledAfterSubscription(Event.BytesRead);
    }

    @Test(timeout = 60000)
    public void testOnConnectionHandlingFailed() throws Exception {
        rule.publisher.onConnectionHandlingFailed(1, TimeUnit.MILLISECONDS, new NullPointerException());

        rule.listener.getTcpDelegate().assertMethodsCalled(ServerEvent.HandlingFailed);
    }

    @Test(timeout = 60000)
    public void testOnConnectionHandlingSuccess() throws Exception {
        rule.publisher.onConnectionHandlingSuccess(1, TimeUnit.MILLISECONDS);

        rule.listener.getTcpDelegate().assertMethodsCalled(ServerEvent.HandlingSuccess);
    }

    @Test(timeout = 60000)
    public void testOnConnectionHandlingStart() throws Exception {
        rule.publisher.onConnectionHandlingStart(1, TimeUnit.MILLISECONDS);

        rule.listener.getTcpDelegate().assertMethodsCalled(ServerEvent.HandlingStart);
    }

    @Test(timeout = 60000)
    public void testOnNewClientConnected() throws Exception {
        rule.publisher.onNewClientConnected();

        rule.listener.getTcpDelegate().assertMethodsCalled(ServerEvent.NewClient);
    }

    @Test(timeout = 60000)
    public void testPublishingEnabled() throws Exception {
        assertThat("Publishing not enabled.", rule.publisher.publishingEnabled(), is(true));
    }

    @Test(timeout = 60000)
    public void testSubscribe() throws Exception {
        rule.listener.getTcpDelegate().getConnDelegate().assertMethodsCalled(Event.Subscribe);
    }

    @Test(timeout = 60000)
    public void testCopy() throws Exception {
        HttpServerEventPublisher copy = rule.publisher.copy(rule.publisher.getTcpDelegate().copy());

        assertThat("Publisher not copied.", copy, is(not(sameInstance(rule.publisher))));
        assertThat("Listeners not copied.", copy.getListeners(), is(not(sameInstance(rule.publisher.getListeners()))));
        assertThat("Delegate not copied.", copy.getTcpDelegate(),
                   is(not(sameInstance(rule.publisher.getTcpDelegate()))));
    }

    public static class PublisherRule extends ExternalResource {

        private HttpServerEventsListenerImpl listener;
        private HttpServerEventPublisher publisher;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    listener = new HttpServerEventsListenerImpl();
                    publisher = new HttpServerEventPublisher(new TcpServerEventPublisher());
                    publisher.subscribe(listener);
                    base.evaluate();
                }
            };
        }
    }
}