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

/*
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.AbstractConnectionToChannelBridge.DrainInputSubscriberBuffer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import rx.Notification;
import rx.Subscriber;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
*/

public class CollaboratedReadInputSubscriberTest {
/*

    @Rule
    public final InputSubscriberRule inputSubscriberRule = new InputSubscriberRule();
    public static final Notification<String> ITEM_1 = Notification.createOnNext("Item1");
    public static final Notification<String> ITEM_2 = Notification.createOnNext("Item2");
    public static final Notification<String> ITEM_3 = Notification.createOnNext("Item3");
    public static final Notification<String> ERROR = Notification.createOnError(
            new NullPointerException("Dummy exception"));

    @Test
    public void testNoBackpressure() throws Exception {
        inputSubscriberRule.requestNext(Long.MAX_VALUE); // No backpressure.
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());
        assertThat("Requested value changed after on next.", inputSubscriberRule.getSubscriber().getRequested(),
                   is(Long.MAX_VALUE));
    }

    @Test
    public void testRequestMoreThanAvailable() throws Exception {
        inputSubscriberRule.requestNext(2);
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(), contains(ITEM_1));

        inputSubscriberRule.reset();

        inputSubscriberRule.subscriber.onCompleted();

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected item received.", inputSubscriberRule.getNotifications(),
                   contains(Notification.<String>createOnCompleted()));
    }

    @Test
    public void testRequestLessThanAvailable() throws Exception {
        inputSubscriberRule.requestNext(1);
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());
        inputSubscriberRule.subscriber.onNext(ITEM_2.getValue());

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(), contains(ITEM_1));

        inputSubscriberRule.reset();
        inputSubscriberRule.unsubscribe();

        assertThat("Drain items not requested post unsubscribe.", inputSubscriberRule.isDrainRequested(), is(true));
        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(0));
        assertThat("Buffer did not drain on unsubscribe.", inputSubscriberRule.subscriber.getItemsCountInBuffer(),
                   is(0));
    }

    @Test
    public void testBufferedTerminationWithRequest0() throws Exception {
        inputSubscriberRule.requestNext(1);
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());
        inputSubscriberRule.subscriber.onNext(ITEM_2.getValue()); // Makes the termination buffer.
        inputSubscriberRule.subscriber.onCompleted();

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(), contains(ITEM_1));

        inputSubscriberRule.reset();

        inputSubscriberRule.requestNext(1); // Terminal event will be popped from the queue when requested == 0

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(2));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(),
                   contains(ITEM_2, Notification.<String>createOnCompleted()));

    }

    @Test
    public void testBackpressure() throws Exception {
        inputSubscriberRule.requestNext(1);
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());
        inputSubscriberRule.subscriber.onNext(ITEM_2.getValue());
        inputSubscriberRule.subscriber.onNext(ITEM_3.getValue());

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(), contains(ITEM_1));

        inputSubscriberRule.reset(); // Reset state.

        inputSubscriberRule.requestNext(2);

        assertThat("Drain items not requested post request next.", inputSubscriberRule.isDrainRequested(), is(true));
        */
/* Since, the items are already available, read would not be requested. *//*

        assertThat("Unexpected read request count.", inputSubscriberRule.getReadRequestedCount(), is(0));

        inputSubscriberRule.subscriber.onCompleted();

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(3));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(),
                   contains(ITEM_2, ITEM_3, Notification.<String>createOnCompleted()));
    }

    @Test
    public void testOnUnsubscribeDrain() throws Exception {

        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());

        assertThat("Buffer did not drain on unsubscribe.", inputSubscriberRule.subscriber.getItemsCountInBuffer(),
                   is(1));

        inputSubscriberRule.unsubscribe();

        assertThat("Buffer did not drain on unsubscribe.", inputSubscriberRule.subscriber.getItemsCountInBuffer(),
                   is(0));
    }

    @Test
    public void testDrainOnChannelUnregister() throws Exception {
        inputSubscriberRule.requestNext(1);
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());
        inputSubscriberRule.subscriber.onNext(ITEM_2.getValue());

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(), contains(ITEM_1));

        inputSubscriberRule.reset();

        inputSubscriberRule.channel.deregister().sync();

        inputSubscriberRule.requestNext(1);

        */
/*Since, the channel is unregistered, drain event will not be sent.*//*

        assertThat("Drain items requested after channel deregister.", inputSubscriberRule.isDrainRequested(),
                   is(false));
        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(), contains(ITEM_2));

    }

    @Test
    public void testOnErrorNoBuffer() throws Exception {
        inputSubscriberRule.requestNext(1);
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());
        inputSubscriberRule.subscriber.onError(ERROR.getThrowable());

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(2));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(),
                   contains(ITEM_1, ERROR));
    }

    @Test
    public void testOnErrorWithBuffer() throws Exception {
        inputSubscriberRule.requestNext(1);
        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(),
                   contains(ITEM_1));

        inputSubscriberRule.reset();

        inputSubscriberRule.subscriber.onNext(ITEM_2.getValue()); // Cause buffer
        inputSubscriberRule.subscriber.onError(ERROR.getThrowable());

        inputSubscriberRule.requestNext(1);

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(2));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(),
                   contains(ITEM_2, ERROR));
    }

    @Test
    public void testAutoReadOffOnBuffer() throws Exception {
        inputSubscriberRule.channel.config().setAutoRead(true);

        inputSubscriberRule.requestNext(1);

        inputSubscriberRule.subscriber.onNext(ITEM_1.getValue());
        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Unexpected notifications received.", inputSubscriberRule.getNotifications(),
                   contains(ITEM_1));

        inputSubscriberRule.subscriber.onNext(ITEM_2.getValue()); // causes buffer

        assertThat("Mismatch in notifications received and requested.", inputSubscriberRule.getNotifications(),
                   hasSize(1));
        assertThat("Auto-read not turned off on buffer.", inputSubscriberRule.channel.config().isAutoRead(),
                   is(false));
    }

    public static class InputSubscriberRule extends ExternalResource {

        private final EmbeddedChannel channel;
        private final CollaboratedReadInputSubscriber<String> subscriber;
        private final List<Notification<String>> notifications;
        private final List<Object> userEvents;
        private final TestableSubscriber original;
        private int readRequestedCount;

        private InputSubscriberRule() {
            channel = new EmbeddedChannel(){
                @Override
                public Channel read() {
                    readRequestedCount++;
                    return super.read();
                }
            };
            channel.config().setAutoRead(false);
            notifications = new ArrayList<>();
            userEvents = new ArrayList<>();
            original = new TestableSubscriber();
            original.onStart(); // Since, we do not subscribe it to an Observable, onStart does not get called by RxJava.
            subscriber = CollaboratedReadInputSubscriber.<String>create(channel, original);
            channel.pipeline().addLast(new ChannelDuplexHandler() {
                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                    super.userEventTriggered(ctx, evt);
                    userEvents.add(evt);
                    if (evt == DrainInputSubscriberBuffer.INSTANCE) {
                        subscriber.drain();
                    }
                }
            });
        }

        public List<Notification<String>> getNotifications() {
            return notifications;
        }

        public List<Object> getUserEvents() {
            return userEvents;
        }

        public boolean isDrainRequested() {
            channel.runPendingTasks(); // Drain is executed on the eventloop, so need to run the tasks for it to happen.
            return userEvents.contains(DrainInputSubscriberBuffer.INSTANCE);
        }

        public CollaboratedReadInputSubscriber<String> getSubscriber() {
            return subscriber;
        }

        public int getReadRequestedCount() {
            return readRequestedCount;
        }

        public void requestNext(long requested) {
            original.requestMore(requested);
            channel.runPendingTasks(); // Drain is executed on the eventloop
        }

        public void reset() {
            notifications.clear();
            userEvents.clear();
            readRequestedCount = 0;
        }

        public void unsubscribe() {
            original.unsubscribe();
            channel.runPendingTasks();
        }

        private class TestableSubscriber extends Subscriber<String> {

            @Override
            public void onStart() {
                request(0);
            }

            @Override
            public void onCompleted() {
                notifications.add(Notification.<String>createOnCompleted());
            }

            @Override
            public void onError(Throwable e) {
                notifications.add(Notification.<String>createOnError(e));
            }

            @Override
            public void onNext(String next) {
                notifications.add(Notification.createOnNext(next));
            }

            public void requestMore(long requested) {
                request(requested);
            }
        }
    }
*/
}