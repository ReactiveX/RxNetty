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

package io.reactivex.netty.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Nitesh Kant
 */
public class UnicastContentSubjectTest {

    @Test(expected = IllegalStateException.class)
    public void testNoSubscriptions() throws Exception {
        TestScheduler testScheduler = Schedulers.test();
        UnicastContentSubject<String> subject = UnicastContentSubject.create(1, TimeUnit.DAYS, testScheduler);
        subject.onNext("Start the timeout now."); // Since the timeout is scheduled only after content arrival.
        testScheduler.advanceTimeBy(1, TimeUnit.DAYS);
        subject.toBlocking().last(); // Should immediately throw an error.
    }

    @Test(expected = IllegalStateException.class)
    public void testNoSubscriptionsWithOnUnsubscribeAction() throws Exception {
        TestScheduler testScheduler = Schedulers.test();
        OnUnsubscribeAction onUnsub = new OnUnsubscribeAction();
        UnicastContentSubject<String> subject = UnicastContentSubject.create(1, TimeUnit.DAYS, testScheduler,
                                                                             onUnsub);
        subject.onNext("Start the timeout now."); // Since the timeout is scheduled only after content arrival.
        testScheduler.advanceTimeBy(1, TimeUnit.DAYS);
        subject.toBlocking().last(); // Should immediately throw an error.
    }

    @Test(expected = IllegalStateException.class)
    public void testMultiSubscriber() throws Exception {
        UnicastContentSubject<Object> subject = UnicastContentSubject.createWithoutNoSubscriptionTimeout();
        subject.subscribe(Subscribers.empty());
        subject.toBlocking().last();
    }

    @Test
    public void testNoTimeoutPostSubscription() throws Exception {
        TestScheduler testScheduler = Schedulers.test();
        UnicastContentSubject<String> subject = UnicastContentSubject.create(1, TimeUnit.DAYS, testScheduler);
        subject.onNext("Start the timeout now."); // Since the timeout is scheduled only after content arrival.
        final AtomicReference<Throwable> errorOnSubscribe = new AtomicReference<Throwable>();
        final CountDownLatch latch = new CountDownLatch(1);
        subject.subscribe(Actions.empty(), new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                errorOnSubscribe.set(throwable);
                latch.countDown();
            }
        }, new Action0() {
            @Override
            public void call() {
                latch.countDown();
            }
        });

        testScheduler.advanceTimeBy(1, TimeUnit.DAYS);
        subject.onCompleted();

        latch.await(1, TimeUnit.MINUTES);

        Assert.assertNull("Subscription got an error.", errorOnSubscribe.get());
    }

    @Test
    public void testOnUnsubscribeAction() throws Exception {
        OnUnsubscribeAction onUnsubscribeAction = new OnUnsubscribeAction();
        UnicastContentSubject.createWithoutNoSubscriptionTimeout(onUnsubscribeAction).subscribe().unsubscribe();
        Assert.assertTrue("On unsubscribe action not called.", onUnsubscribeAction.isCalled());
    }

    @Test
    public void testBuffer() throws Exception {
        UnicastContentSubject<String> subject = UnicastContentSubject.createWithoutNoSubscriptionTimeout();

        final List<String> data = new ArrayList<String>();
        data.add("Item1");
        data.add("Item2");

        //buffer these
        for (String item : data) {
            subject.onNext(item);
        }
        subject.onCompleted();

        final List<String> items = new ArrayList<String>();
        subject.toBlocking().forEach(new Action1<String>() {
            @Override
            public void call(String item) {
                items.add(item);
            }
        });

        Assert.assertEquals("Unexpected onNext calls", data, items);
    }

    @Test
    public void testByteBufReleaseWithNoTimeout() throws Exception {
        UnicastContentSubject<ByteBuf> subject = UnicastContentSubject.createWithoutNoSubscriptionTimeout();
        ByteBuf buffer = Unpooled.buffer();
        Assert.assertEquals("Created byte buffer not retained.", 1, buffer.refCnt());
        subject.onNext(buffer);
        subject.onCompleted();
        final AtomicInteger byteBufRefCnt = new AtomicInteger(-1);

        ByteBuf last = subject.doOnNext(new Action1<ByteBuf>() {
            @Override
            public void call(ByteBuf byteBuf) {
                byteBufRefCnt.set(byteBuf.refCnt());
                byteBuf.release();// Simulate consumption as ByteBuf refcount is 1 when created.
            }
        }).toBlocking().last();

        Assert.assertEquals("Unexpected ByteBuf ref count when received.", 2, byteBufRefCnt.get());
        Assert.assertSame("Unexpected byte buffer received.", buffer, last);
        Assert.assertEquals("Byte buffer not released.", 0, last.refCnt());
    }

    @Test
    public void testByteBufReleaseWithTimeout() throws Exception {
        TestScheduler testScheduler = Schedulers.test();
        UnicastContentSubject<ByteBuf> subject = UnicastContentSubject.create(100, TimeUnit.MILLISECONDS,
                                                                              testScheduler);
        ByteBuf buffer = Unpooled.buffer();

        subject.onNext(buffer);
        buffer.release(); // Simulatimg auto-release behavior in rxnetty today. (Issue: https://github.com/ReactiveX/RxNetty/issues/264)
        Assert.assertEquals("Byte buffer not retained on buffering by subject.", 1, buffer.refCnt());

        subject.onCompleted();

        testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Byte buffer not fully released", 0, buffer.refCnt());
    }

    @Test
    public void testUnsubscribe() throws Exception {
        final TestScheduler testScheduler = Schedulers.test();
        UnicastContentSubject<Long> subject = UnicastContentSubject.createWithoutNoSubscriptionTimeout();
        subject.onNext(1l);
        subject.onCompleted();

        final AtomicBoolean outerUnsubscribe = new AtomicBoolean();
        final AtomicBoolean innerUnsubscribe = new AtomicBoolean();
        final AtomicBoolean sourceCompleted = new AtomicBoolean();

        subject.doOnCompleted(new Action0() {
            @Override
            public void call() {
                sourceCompleted.set(true);
            }
        }).flatMap(new Func1<Long, Observable<Long>>() {
            @Override
            public Observable<Long> call(Long aLong) {
                return Observable.interval(1, TimeUnit.SECONDS, testScheduler)
                                 .doOnUnsubscribe(new Action0() {
                                     @Override
                                     public void call() {
                                         innerUnsubscribe.set(true);
                                     }
                                 });
            }
        }).take(1).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                outerUnsubscribe.set(true);
            }
        }).toBlocking().toFuture(); // This subscribes to the subject

        Assert.assertTrue("Source did not complete on subscription.", sourceCompleted.get());
        Assert.assertFalse("Inner flatmap got unsubscribed on source completion.", innerUnsubscribe.get());

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        Assert.assertTrue("Outer subscriber did not unsubscribe on inner completion.", outerUnsubscribe.get());
        Assert.assertTrue("Inner subscriber did not unsubscribe on inner completion.", innerUnsubscribe.get());
    }

    private static class OnUnsubscribeAction implements Action0 {

        private volatile boolean called;

        public OnUnsubscribeAction() {
        }

        @Override
        public void call() {
            called = true;
        }

        public boolean isCalled() {
            return called;
        }
    }
}
