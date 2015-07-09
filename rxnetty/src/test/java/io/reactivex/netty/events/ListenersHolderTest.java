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
package io.reactivex.netty.events;

import io.reactivex.netty.events.ListenersHolderRule.ListenerWithSub;
import org.junit.Rule;
import org.junit.Test;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Action4;
import rx.functions.Action5;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ListenersHolderTest {

    @Rule
    public final ListenersHolderRule holderRule = new ListenersHolderRule();

    @Test(timeout = 60000)
    public void testSubscribe() throws Exception {

        ListenerWithSub l = holderRule.addAListener();

        holderRule.assertListenerAdded(l.listener);

        l.subscription.unsubscribe();

        assertThat("Listener not removed on unsubscribe.", holderRule.getHolder().getAllListeners(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testMultipleListeners() throws Exception {
        final TestEventListener listener1 = new TestEventListener();
        final TestEventListener listener2 = new TestEventListener();
        Subscription subscription1 = holderRule.getHolder().subscribe(listener1);
        Subscription subscription2 = holderRule.getHolder().subscribe(listener2);

        holderRule.assertListenerAdded(listener1, listener2);

        subscription1.unsubscribe();

        assertThat("Listener not removed on unsubscribe.", holderRule.getHolder().getAllListeners(), hasSize(1));
        assertThat("Listener not removed on unsubscribe.", holderRule.getHolder().getAllListeners(),
                   not(contains(listener1)));
        assertThat("Listener not removed on unsubscribe.", holderRule.getHolder().getAllListeners(),
                   contains(listener2));

        subscription2.unsubscribe();

        assertThat("Listener not removed on unsubscribe.", holderRule.getHolder().getAllListeners(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testPublishingEnabled() throws Exception {

        assertThat("Publishing enabled with no listeners", holderRule.getHolder().publishingEnabled(), is(false));

        ListenerWithSub l1 = holderRule.addAListener();

        assertThat("Publishing disabled with a listener", holderRule.getHolder().publishingEnabled(), is(true));

        l1.subscription.unsubscribe();

        assertThat("Listener not removed on unsubscribe.", holderRule.getHolder().getAllListeners(), is(empty()));

        assertThat("Publishing enabled post listener unsubscribe", holderRule.getHolder().publishingEnabled(), is(false));
    }

    @Test(timeout = 60000)
    public void testDispose() throws Exception {
        ListenerWithSub l = holderRule.addAListener();
        holderRule.getHolder().dispose();

        assertThat("On complete not called on dispose.", l.listener.getOnCompletedCount(), is(1));
        assertThat("Listener not unsubscribed on dispose.", l.subscription.isUnsubscribed(), is(true));

        assertThat("Listener not removed on dispose.", holderRule.getHolder().getAllListeners(),
                   not(contains(l.listener)));
    }

    @Test(timeout = 60000)
    public void testDisposeWithExceptions() throws Exception {
        final TestEventListener listener1 = new TestEventListener(true);
        final TestEventListener listener2 = new TestEventListener();
        Subscription subscription1 = holderRule.getHolder().subscribe(listener1);
        Subscription subscription2 = holderRule.getHolder().subscribe(listener2);

        assertThat("Listeners not added.", holderRule.getHolder().getAllListeners(), hasSize(2));
        assertThat("Listeners not added.", holderRule.getHolder().getAllListeners(), contains(listener1, listener2));

        try {
            holderRule.getHolder().dispose();
            throw new AssertionError("Error not thrown on dispose.");
        } catch (Exception e) {
            // Expected.
        }

        assertThat("First listener not completed.", listener1.getOnCompletedCount(), is(1));
        assertThat("Second listener not completed.", listener2.getOnCompletedCount(), is(1));

        assertThat("First listener not unsubscribed.", subscription1.isUnsubscribed(), is(true));
        assertThat("Second listener not unsubscribed.", subscription2.isUnsubscribed(), is(true));

        assertThat("Listeners not removed post dispose.", holderRule.getHolder().getAllListeners(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testInvokeListeners() throws Exception {
        final TestEventListener listener = new TestEventListener();
        holderRule.getHolder().subscribe(listener);
        holderRule.assertListenerAdded(listener);

        holderRule.getHolder().invokeListeners(new Action1<TestEventListener>() {
            @Override
            public void call(TestEventListener testEventListener) {
                testEventListener.anEvent();
            }
        });

        assertThat("Listener not invoked.", listener.getEventInvocationCount(), is(1));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersMulti() throws Exception {
        final TestEventListener listener1 = new TestEventListener();
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        holderRule.getHolder().invokeListeners(new Action1<TestEventListener>() {
            @Override
            public void call(TestEventListener testEventListener) {
                testEventListener.anEvent();
            }
        });

        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked.", listener2.getEventInvocationCount(), is(1));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersRaiseException() throws Exception {
        final TestEventListener listener1 = new TestEventListener(true);
        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener1);
        holderRule.getHolder().subscribe(listener2);

        assertThat("Listeners not added.", holderRule.getHolder().getAllListeners(), hasSize(2));
        assertThat("Listeners not added.", holderRule.getHolder().getAllListeners(), contains(listener1, listener2));

        holderRule.getHolder().invokeListeners(new Action1<TestEventListener>() {
            @Override
            public void call(TestEventListener testEventListener) {
                testEventListener.anEvent();
            }
        });

        assertThat("First listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithDuration() throws Exception {
        final TestEventListener listener = new TestEventListener();
        holderRule.getHolder().subscribe(listener);
        holderRule.assertListenerAdded(listener);

        holderRule.getHolder().invokeListeners(new Action3<TestEventListener, Long, TimeUnit>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit) {
                testEventListener.anEventWithDuration(duration, timeUnit);
            }
        }, 1, TimeUnit.MICROSECONDS);

        assertThat("Listener not invoked.", listener.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener.getTimeUnit(), is(TimeUnit.MICROSECONDS));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersMultiWithDuration() throws Exception {
        final TestEventListener listener1 = new TestEventListener();
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        holderRule.getHolder().invokeListeners(new Action3<TestEventListener, Long, TimeUnit>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit) {
                testEventListener.anEventWithDuration(duration, timeUnit);
            }
        }, 1, TimeUnit.MICROSECONDS);


        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener1.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener1.getTimeUnit(), is(TimeUnit.MICROSECONDS));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with duration.", listener2.getDuration(), is(1L));
        assertThat("Second listener not invoked with time unit.", listener2.getTimeUnit(), is(TimeUnit.MICROSECONDS));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithDurationRaiseException() throws Exception {
        final TestEventListener listener1 = new TestEventListener(true);
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        holderRule.getHolder().invokeListeners(new Action3<TestEventListener, Long, TimeUnit>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit) {
                testEventListener.anEventWithDuration(duration, timeUnit);
            }
        }, 1, TimeUnit.MICROSECONDS);


        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener1.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener1.getTimeUnit(), is(TimeUnit.MICROSECONDS));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with duration.", listener2.getDuration(), is(1L));
        assertThat("Second listener not invoked with time unit.", listener2.getTimeUnit(), is(TimeUnit.MICROSECONDS));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithDurationAndError() throws Exception {
        final TestEventListener listener = new TestEventListener();
        holderRule.getHolder().subscribe(listener);
        holderRule.assertListenerAdded(listener);

        final Throwable expected = new NullPointerException();
        holderRule.getHolder().invokeListeners(new Action4<TestEventListener, Long, TimeUnit, Throwable>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit, Throwable t) {
                testEventListener.anEventWithDurationAndError(duration, timeUnit, t);
            }
        }, 1, TimeUnit.MICROSECONDS, expected);

        assertThat("Listener not invoked.", listener.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Listener not invoked with error.", listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersMultiWithDurationAndError() throws Exception {
        final TestEventListener listener1 = new TestEventListener();
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        final Throwable expected = new NullPointerException();
        holderRule.getHolder().invokeListeners(new Action4<TestEventListener, Long, TimeUnit, Throwable>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit, Throwable t) {
                testEventListener.anEventWithDurationAndError(duration, timeUnit, t);
            }
        }, 1, TimeUnit.MICROSECONDS, expected);


        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener1.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener1.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Listener not invoked with error.", listener1.getRecievedError(), is(expected));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with duration.", listener2.getDuration(), is(1L));
        assertThat("Second listener not invoked with time unit.", listener2.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Second listener not invoked with error.", listener2.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithDurationAndErrorRaiseException() throws Exception {
        final TestEventListener listener1 = new TestEventListener(true);
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        final Throwable expected = new NullPointerException();
        holderRule.getHolder().invokeListeners(new Action4<TestEventListener, Long, TimeUnit, Throwable>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit, Throwable t) {
                testEventListener.anEventWithDurationAndError(duration, timeUnit, t);
            }
        }, 1, TimeUnit.MICROSECONDS, expected);


        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener1.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener1.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Listener not invoked with error.", listener1.getRecievedError(), is(expected));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with duration.", listener2.getDuration(), is(1L));
        assertThat("Second listener not invoked with time unit.", listener2.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Second listener not invoked with error.", listener2.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithDurationAndArg() throws Exception {
        final TestEventListener listener = new TestEventListener();
        holderRule.getHolder().subscribe(listener);
        holderRule.assertListenerAdded(listener);

        final String arg = "doom";
        holderRule.getHolder().invokeListeners(new Action4<TestEventListener, Long, TimeUnit, String>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit, String arg) {
                testEventListener.anEventWithDurationAndArg(duration, timeUnit, arg);
            }
        }, 1, TimeUnit.MICROSECONDS, arg);

        assertThat("Listener not invoked.", listener.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Listener not invoked with argument.", listener.getArg(), is(arg));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersMultiWithDurationAndArg() throws Exception {
        final TestEventListener listener1 = new TestEventListener();
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        final String arg = "doom";
        holderRule.getHolder().invokeListeners(new Action4<TestEventListener, Long, TimeUnit, String>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit, String arg) {
                testEventListener.anEventWithDurationAndArg(duration, timeUnit, arg);
            }
        }, 1, TimeUnit.MICROSECONDS, arg);

        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener1.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener1.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Listener not invoked with argument.", listener1.getArg(), is(arg));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with duration.", listener2.getDuration(), is(1L));
        assertThat("Second listener not invoked with time unit.", listener2.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Second listener not invoked with argument.", listener2.getArg(), is(arg));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersMultiWithDurationErrorAndArg() throws Exception {
        final TestEventListener listener1 = new TestEventListener();
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        final Object event = "doom";
        final Throwable expected = new NullPointerException();
        holderRule.getHolder().invokeListeners(new Action5<TestEventListener, Long, TimeUnit, Throwable, Object>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit, Throwable throwable,
                             Object event) {
                testEventListener.onCustomEvent(event, duration, timeUnit, throwable);
            }
        }, 1, TimeUnit.MICROSECONDS, expected, event);

        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener1.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener1.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Listener not invoked with error.", listener1.getRecievedError(), is(expected));
        assertThat("Listener not invoked with argument.", listener1.getCustomEvent(), is(event));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with duration.", listener2.getDuration(), is(1L));
        assertThat("Second listener not invoked with time unit.", listener2.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Second listener not invoked with error.", listener2.getRecievedError(), is(expected));
        assertThat("Second listener not invoked with argument.", listener2.getCustomEvent(), is(event));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithDurationArgRaiseException() throws Exception {
        final TestEventListener listener1 = new TestEventListener(true);
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        final String arg = "doom";
        holderRule.getHolder().invokeListeners(new Action4<TestEventListener, Long, TimeUnit, String>() {
            @Override
            public void call(TestEventListener testEventListener, Long duration, TimeUnit timeUnit, String arg) {
                testEventListener.anEventWithDurationAndArg(duration, timeUnit, arg);
            }
        }, 1, TimeUnit.MICROSECONDS, arg);

        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with duration.", listener1.getDuration(), is(1L));
        assertThat("Listener not invoked with time unit.", listener1.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Listener not invoked with argument.", listener1.getArg(), is(arg));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with duration.", listener2.getDuration(), is(1L));
        assertThat("Second listener not invoked with time unit.", listener2.getTimeUnit(), is(TimeUnit.MICROSECONDS));
        assertThat("Second listener not invoked with argument.", listener2.getArg(), is(arg));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithArg() throws Exception {
        final TestEventListener listener = new TestEventListener();
        holderRule.getHolder().subscribe(listener);
        holderRule.assertListenerAdded(listener);

        final String arg = "doom";
        holderRule.getHolder().invokeListeners(new Action2<TestEventListener, String>() {
            @Override
            public void call(TestEventListener testEventListener, String arg) {
                testEventListener.anEventWithArg(arg);
            }
        }, arg);

        assertThat("Listener not invoked.", listener.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with argument.", listener.getArg(), is(arg));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithExceptionAndArg() throws Exception {
        final TestEventListener listener = new TestEventListener();
        holderRule.getHolder().subscribe(listener);
        holderRule.assertListenerAdded(listener);

        final Object event = "doom";
        final Throwable expected = new NullPointerException();
        holderRule.getHolder().invokeListeners(new Action3<TestEventListener, Throwable, Object>() {
            @Override
            public void call(TestEventListener testEventListener, Throwable throwable, Object arg) {
                testEventListener.onCustomEvent(arg, throwable);
            }
        }, expected, event);

        assertThat("Listener not invoked.", listener.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with argument.", listener.getCustomEvent(), equalTo(event));
        assertThat("Listener not invoked with exception.", listener.getRecievedError(), is(expected));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersMultiWithArg() throws Exception {
        final TestEventListener listener1 = new TestEventListener();
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        final String arg = "doom";
        holderRule.getHolder().invokeListeners(new Action2<TestEventListener, String>() {
            @Override
            public void call(TestEventListener testEventListener, String arg) {
                testEventListener.anEventWithArg(arg);
            }
        }, arg);

        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with argument.", listener1.getArg(), is(arg));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with argument.", listener2.getArg(), is(arg));
    }

    @Test(timeout = 60000)
    public void testInvokeListenersWithArgAndRaiseException() throws Exception {
        final TestEventListener listener1 = new TestEventListener(true);
        holderRule.getHolder().subscribe(listener1);

        final TestEventListener listener2 = new TestEventListener();
        holderRule.getHolder().subscribe(listener2);
        holderRule.assertListenerAdded(listener1, listener2);

        final String arg = "doom";
        holderRule.getHolder().invokeListeners(new Action2<TestEventListener, String>() {
            @Override
            public void call(TestEventListener testEventListener, String arg) {
                testEventListener.anEventWithArg(arg);
            }
        }, arg);

        assertThat("Listener not invoked.", listener1.getEventInvocationCount(), is(1));
        assertThat("Listener not invoked with argument.", listener1.getArg(), is(arg));

        assertThat("Second listener not invoked.", listener2.getEventInvocationCount(), is(1));
        assertThat("Second listener not invoked with argument.", listener2.getArg(), is(arg));
    }

    @Test(timeout = 60000)
    public void testDuplicateListeners() throws Exception {
        ListenerWithSub l = holderRule.addAListener();

        holderRule.assertListenerAdded(l.listener);

        holderRule.getHolder().subscribe(l.listener);

        assertThat("Duplicate listener added.", holderRule.getHolder().getActualListenersList(), hasSize(1));

        l.subscription.unsubscribe();

        assertThat("Listener not removed on unsubscribe.", holderRule.getHolder().getAllListeners(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testCopy() throws Exception {
        final TestEventListener listener = new TestEventListener();
        Subscription subscription = holderRule.getHolder().subscribe(listener);
        holderRule.assertListenerAdded(listener);

        final ListenersHolder<TestEventListener> copy = holderRule.getHolder().copy();

        assertThat("Holder not copied", copy, is(not(holderRule.getHolder())));
        assertThat("Listeners list not copied", copy.getActualListenersList(),
                   not(sameInstance(holderRule.getHolder().getActualListenersList())));

        final Collection<TestEventListener> allListenersCopied = copy.getAllListeners();

        assertThat("Registered listeners not copied", allListenersCopied, contains(listener));

        subscription.unsubscribe();

        assertThat("Not removed from copy on unsubscribe.", copy.getAllListeners(), not(contains(listener)));
    }

    public static class TestEventListener implements EventListener {

        private int onCompletedCount;
        private int eventInvocationCount;
        private final boolean raiseErrorOnAllInvocations;
        private long duration;
        private TimeUnit timeUnit;
        private Throwable recievedError;
        private String arg;
        private Object customEvent;

        public TestEventListener() {
            this(false);
        }

        public TestEventListener(boolean raiseErrorOnAllInvocations) {
            this.raiseErrorOnAllInvocations = raiseErrorOnAllInvocations;
        }

        public void anEvent() {
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        public void anEventWithArg(String arg) {
            eventInvocationCount++;
            this.arg = arg;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        public void anEventWithDuration(long duration, TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = timeUnit;
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        public void anEventWithDurationAndError(long duration, TimeUnit timeUnit, Throwable t) {
            this.duration = duration;
            this.timeUnit = timeUnit;
            recievedError = t;
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        public void anEventWithDurationAndArg(long duration, TimeUnit timeUnit, String arg) {
            this.duration = duration;
            this.timeUnit = timeUnit;
            this.arg = arg;
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        @Override
        public void onCompleted() {
            onCompletedCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        @Override
        public void onCustomEvent(Object event) {
            customEvent = event;
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        @Override
        public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = timeUnit;
            customEvent = event;
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        @Override
        public void onCustomEvent(Object event, Throwable throwable) {
            customEvent = event;
            recievedError = throwable;
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        @Override
        public void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable) {
            this.duration = duration;
            this.timeUnit = timeUnit;
            recievedError = throwable;
            customEvent = event;
            eventInvocationCount++;
            if (raiseErrorOnAllInvocations) {
                throw new IllegalStateException("Deliberate exception.");
            }
        }

        public int getOnCompletedCount() {
            return onCompletedCount;
        }

        public int getEventInvocationCount() {
            return eventInvocationCount;
        }

        public long getDuration() {
            return duration;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public Throwable getRecievedError() {
            return recievedError;
        }

        public String getArg() {
            return arg;
        }

        public Object getCustomEvent() {
            return customEvent;
        }
    }

}