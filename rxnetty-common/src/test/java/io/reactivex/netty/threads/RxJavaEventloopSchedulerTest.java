/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.threads;

import io.netty.channel.nio.NioEventLoopGroup;
import io.reactivex.netty.threads.RxJavaEventloopScheduler.EventloopWorker;
import org.junit.Test;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.observers.TestSubscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class RxJavaEventloopSchedulerTest {

    @Test(timeout = 60000)
    public void testScheduleNow() throws Exception {
        RxJavaEventloopScheduler scheduler = new RxJavaEventloopScheduler(new NioEventLoopGroup());
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        Observable.range(1, 1)
                  .observeOn(scheduler)
                  .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        testSubscriber.assertValue(1);
    }

    @Test(timeout = 60000)
    public void testScheduleDelay() throws Exception {
        RxJavaEventloopScheduler scheduler = new RxJavaEventloopScheduler(new NioEventLoopGroup());
        TestSubscriber<Long> testSubscriber = new TestSubscriber<>();

        Observable.timer(1, TimeUnit.MILLISECONDS, scheduler)
                  .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        testSubscriber.assertValue(0L);
    }

    @Test(timeout = 60000)
    public void testRemoveNonDelayedTasks() throws Exception {
        RxJavaEventloopScheduler scheduler = new RxJavaEventloopScheduler(new NioEventLoopGroup());

        final EventloopWorker worker = (EventloopWorker) scheduler.createWorker();

        assertThat("New worker already has subscriptions.", worker.hasScheduledSubscriptions(), is(false));

        final AtomicBoolean isScheduledBeforeExecute = new AtomicBoolean();
        final CountDownLatch executed = new CountDownLatch(1);

        Subscription subscription = worker.schedule(new Action0() {
            @Override
            public void call() {
                isScheduledBeforeExecute.set(worker.hasScheduledSubscriptions());
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        executed.countDown();
                    }
                });
            }
        });

        executed.await();

        assertThat("No scheduled subscriptions on executing the action.", isScheduledBeforeExecute.get(), is(true));
        assertThat("Action not unsubscribed.", subscription.isUnsubscribed(), is(true));

        assertThat("Subscription not removed post execution.", worker.hasScheduledSubscriptions(), is(false));
    }

    @Test(timeout = 60000)
    public void testRemoveDelayedTasks() throws Exception {
        RxJavaEventloopScheduler scheduler = new RxJavaEventloopScheduler(new NioEventLoopGroup());

        final EventloopWorker worker = (EventloopWorker) scheduler.createWorker();

        assertThat("New worker already has subscriptions.", worker.hasDelayScheduledSubscriptions(), is(false));

        final AtomicBoolean isScheduledBeforeExecute = new AtomicBoolean();
        final CountDownLatch executed = new CountDownLatch(1);

        Subscription subscription = worker.schedule(new Action0() {
            @Override
            public void call() {
                isScheduledBeforeExecute.set(worker.hasDelayScheduledSubscriptions());
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        executed.countDown();
                    }
                });
            }
        }, 1, TimeUnit.MILLISECONDS);

        executed.await();

        assertThat("No scheduled subscriptions on executing the action.", isScheduledBeforeExecute.get(), is(true));
        assertThat("Action not unsubscribed.", subscription.isUnsubscribed(), is(true));

        assertThat("Subscription not removed post execution.", worker.hasDelayScheduledSubscriptions(), is(false));
    }

    @Test(timeout = 60000)
    public void testUnsubscribeDelayedTasks() throws Exception {
        RxJavaEventloopScheduler scheduler = new RxJavaEventloopScheduler(new NioEventLoopGroup());

        final EventloopWorker worker = (EventloopWorker) scheduler.createWorker();

        assertThat("New worker already has subscriptions.", worker.hasDelayScheduledSubscriptions(), is(false));

        Subscription subscription = worker.schedule(new Action0() {
            @Override
            public void call() {
            }
        }, 100, TimeUnit.DAYS);

        assertThat("No subscriptions post schedule.", worker.hasDelayScheduledSubscriptions(), is(true));

        subscription.unsubscribe();

        assertThat("Subscription not removed post cancellation.", worker.hasDelayScheduledSubscriptions(), is(false));
    }
}