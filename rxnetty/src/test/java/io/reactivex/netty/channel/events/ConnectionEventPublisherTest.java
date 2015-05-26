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
package io.reactivex.netty.channel.events;

import io.reactivex.netty.channel.events.ConnectionEventPublisherTest.ConnectionEventListenerImpl.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ConnectionEventPublisherTest {

    @Rule
    public final PublisherRule rule = new PublisherRule();

    @Test(timeout = 60000)
    public void testOnByteRead() throws Exception {
        rule.publisher.onByteRead(1);
        rule.listener.assertMethodsCalledAfterSubscription(Event.BytesRead);

        assertThat("Listener not called with bytes read.", rule.listener.getBytesRead(), is(1L));
    }

    @Test(timeout = 60000)
    public void testOnFlushStart() throws Exception {
        rule.publisher.onFlushStart();
        rule.listener.assertMethodsCalledAfterSubscription(Event.FlushStart);
    }

    @Test(timeout = 60000)
    public void testOnFlushSuccess() throws Exception {
        rule.publisher.onFlushSuccess(1, TimeUnit.MILLISECONDS);
        rule.listener.assertMethodsCalledAfterSubscription(Event.FlushSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnFlushFailed() throws Exception {
        final Throwable expected = new NullPointerException("Deliberate");
        rule.publisher.onFlushFailed(1, TimeUnit.MILLISECONDS, expected);
        rule.listener.assertMethodsCalledAfterSubscription(Event.FlushFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnWriteStart() throws Exception {
        rule.publisher.onWriteStart();
        rule.listener.assertMethodsCalledAfterSubscription(Event.WriteStart);
    }

    @Test(timeout = 60000)
    public void testOnWriteSuccess() throws Exception {
        rule.publisher.onWriteSuccess(1, TimeUnit.MILLISECONDS, 10);
        rule.listener.assertMethodsCalledAfterSubscription(Event.WriteSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with bytes written.", rule.listener.getBytesWritten(), is(10L));
    }

    @Test(timeout = 60000)
    public void testOnWriteFailed() throws Exception {
        final Throwable expected = new NullPointerException("Deliberate");
        rule.publisher.onWriteFailed(1, TimeUnit.MILLISECONDS, expected);
        rule.listener.assertMethodsCalledAfterSubscription(Event.WriteFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseStart() throws Exception {
        rule.publisher.onConnectionCloseStart();
        rule.listener.assertMethodsCalledAfterSubscription(Event.CloseStart);
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseSuccess() throws Exception {
        rule.publisher.onConnectionCloseSuccess(1, TimeUnit.MILLISECONDS);
        rule.listener.assertMethodsCalledAfterSubscription(Event.CloseSuccess);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 60000)
    public void testOnConnectionCloseFailed() throws Exception {
        final Throwable expected = new NullPointerException("Deliberate");
        rule.publisher.onConnectionCloseFailed(1, TimeUnit.MILLISECONDS, expected);
        rule.listener.assertMethodsCalledAfterSubscription(Event.CloseFailed);

        assertThat("Listener not called with duration.", rule.listener.getDuration(), is(1L));
        assertThat("Listener not called with time unit.", rule.listener.getTimeUnit(), is(TimeUnit.MILLISECONDS));
        assertThat("Listener not called with error.", rule.listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testSubscribe() throws Exception {
        rule.listener.assertMethodsCalled(ConnectionEventListenerImpl.Event.Subscribe);
    }

    @Test(timeout = 60000)
    public void testPublishingEnabled() throws Exception {
        assertThat("Publishing not enabled.", rule.publisher.publishingEnabled(), is(true));
    }

    @Test(timeout = 60000)
    public void testCopy() throws Exception {
        ConnectionEventPublisher<ConnectionEventListenerImpl> copy = rule.publisher.copy();

        assertThat("Publisher not copied.", copy, is(not(sameInstance(rule.publisher))));
        assertThat("Listeners not copied.", copy.getListeners(), is(not(sameInstance(rule.publisher.getListeners()))));
    }

    public static class PublisherRule extends ExternalResource {

        private ConnectionEventListenerImpl listener;
        private ConnectionEventPublisher<ConnectionEventListenerImpl> publisher;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    listener = new ConnectionEventListenerImpl();
                    publisher = new ConnectionEventPublisher<>();
                    publisher.subscribe(listener);
                    base.evaluate();
                }
            };
        }
    }

    public static class ConnectionEventListenerImpl extends ConnectionEventListener {

        public enum Event {
            BytesRead, FlushStart, FlushSuccess, FlushFailed, WriteStart, WriteSuccess, WriteFailed, CloseStart,
            CloseSuccess, CloseFailed, Complete, Subscribe
        }

        private final List<Event> methodsCalled = new ArrayList<>();
        private long bytesRead;
        private long duration;
        private TimeUnit timeUnit;
        private long bytesWritten;
        private Throwable recievedError;

        @Override
        public void onByteRead(long bytesRead) {
            methodsCalled.add(Event.BytesRead);
            this.bytesRead = bytesRead;
        }

        @Override
        public void onFlushStart() {
            methodsCalled.add(Event.FlushStart);
        }

        @Override
        public void onFlushSuccess(long duration, TimeUnit timeUnit) {
            methodsCalled.add(Event.FlushSuccess);
            this.duration = duration;
            this.timeUnit = timeUnit;
        }

        @Override
        public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            methodsCalled.add(Event.FlushFailed);
            this.duration = duration;
            this.timeUnit = timeUnit;
            recievedError = throwable;
        }

        @Override
        public void onWriteStart() {
            methodsCalled.add(Event.WriteStart);
        }

        @Override
        public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
            methodsCalled.add(Event.WriteSuccess);
            this.duration = duration;
            this.timeUnit = timeUnit;
            this.bytesWritten = bytesWritten;
        }

        @Override
        public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            methodsCalled.add(Event.WriteFailed);
            this.duration = duration;
            this.timeUnit = timeUnit;
            recievedError = throwable;
        }

        @Override
        public void onConnectionCloseStart() {
            methodsCalled.add(Event.CloseStart);
        }

        @Override
        public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
            methodsCalled.add(Event.CloseSuccess);
            this.duration = duration;
            this.timeUnit = timeUnit;
        }

        @Override
        public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
            methodsCalled.add(Event.CloseFailed);
            this.duration = duration;
            this.timeUnit = timeUnit;
            this.recievedError = recievedError;
        }

        @Override
        public void onCompleted() {
            methodsCalled.add(Event.Complete);
        }

        @Override
        public void onSubscribe() {
            methodsCalled.add(Event.Subscribe);
        }

        public void assertMethodsCalled(Event... events) {
            assertThat("Unexpected methods called count. Methods called: " + methodsCalled, methodsCalled, hasSize(events.length));
            assertThat("Unexpected methods called.", methodsCalled, contains(events));
        }

        public void assertMethodsCalledAfterSubscription(Event... events) {
            Event[] toCheck = new Event[events.length + 1];
            System.arraycopy(events, 0, toCheck, 1, events.length);
            toCheck[0] = Event.Subscribe;
            assertThat("Unexpected methods called count.", methodsCalled, hasSize(toCheck.length));
            assertThat("Unexpected methods called.", methodsCalled, contains(toCheck));
        }

        public List<Event> getMethodsCalled() {
            return methodsCalled;
        }

        public long getBytesRead() {
            return bytesRead;
        }

        public long getDuration() {
            return duration;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public long getBytesWritten() {
            return bytesWritten;
        }

        public Throwable getRecievedError() {
            return recievedError;
        }
    }

}