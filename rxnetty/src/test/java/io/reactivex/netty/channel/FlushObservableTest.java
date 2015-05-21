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

import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class FlushObservableTest {

    @Rule
    public final FlushObservableRule flushObservableRule = new FlushObservableRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test(timeout = 60000)
    public void testSingleWriteAndFlush() throws Exception {
        _writeFlushAndAssert("msg1");
    }

    @Test(timeout = 60000)
    public void testMultipleWriteAndFlush() throws Exception {
        _writeFlushAndAssert("msg1", "msg2", "msg3");
    }

    @Test(timeout = 60000)
    public void testMultipleFlush() throws Exception {
        _writeFlushAndAssert("msg1");
        _writeFlushAndAssert("msg2");
    }

    @Test(timeout = 60000)
    public void testWriteFail() throws Exception {
        ChannelPromise newPromise = flushObservableRule.channel.newPromise();
        flushObservableRule.observable.add(newPromise);

        newPromise.setFailure(new IllegalArgumentException("Inducing failure"));

        List<Notification<Void>> notifications = flushObservableRule.flushAndGetAllNotification();

        assertThat("Unexpected notifications count.", notifications, hasSize(1));
        assertThat("Unexpected notification.", notifications,
                   contains(Notification.<Void>createOnError(newPromise.cause())));
    }

    @Test(timeout = 60000)
    public void testCancelPendingWrites() throws Throwable {

        thrown.expectCause(isA(CancellationException.class));

        flushObservableRule.writeAll("msg1");
        flushObservableRule.observable.cancelPendingFutures(false);
        flushObservableRule.observable.toBlocking()
                                      .toFuture()
                                      .get(1, TimeUnit.MINUTES);
    }

    @Test(timeout = 60000)
    public void testFlushWithNoWrites() throws Exception {
        List<Notification<Void>> notifications = flushObservableRule.flushAndGetAllNotification();
        assertThat("Unexpected notifications count.", notifications, hasSize(1));
        assertThat("Unexpected notification.", notifications, contains(Notification.<Void>createOnCompleted()));
    }

    private void _writeFlushAndAssert(String... msgsToWrite)
            throws InterruptedException, ExecutionException, TimeoutException {

        flushObservableRule.writeAll(msgsToWrite);

        List<Notification<Void>> notifications = flushObservableRule.flushAndGetAllNotification();

        assertThat("Unexpected notifications count.", notifications, hasSize(1));
        assertThat("Unexpected notification.", notifications, contains(Notification.<Void>createOnCompleted()));

        List<String> flushedMessages = flushObservableRule.removeAndGetFlushedItemsAsStrings();

        assertThat("Unexpected messages count.", flushedMessages, hasSize(msgsToWrite.length));
        assertThat("Unexpected messages written to the channel.", flushedMessages, contains(msgsToWrite));
    }

    public static class FlushObservableRule extends ExternalResource {

        private FlushObservable observable;
        private EmbeddedChannel channel;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    channel = new EmbeddedChannel();
                    observable = FlushObservable.create(new MetricEventsSubject<ClientMetricsEvent<?>>(),
                                                        ClientChannelMetricEventProvider.INSTANCE, channel);
                    base.evaluate();
                }
            };
        }

        public void writeAll(String... msgsToWrite) {
            for (String msg: msgsToWrite) {
                observable.add(channel.write(msg));
            }
        }

        public List<Notification<Void>> flushAndGetAllNotification()
                throws InterruptedException, ExecutionException, TimeoutException {
            return observable.materialize()
                             .toList()
                             .toBlocking()
                             .toFuture()
                             .get(1, TimeUnit.MINUTES);
        }

        public List<String> removeAndGetFlushedItemsAsStrings() {
            List<String> flushedMessages = new ArrayList<>();
            Object flushedMessage;
            while ((flushedMessage = channel.readOutbound()) != null) {
                flushedMessages.add(flushedMessage.toString());
            }
            return flushedMessages;
        }
    }
}
