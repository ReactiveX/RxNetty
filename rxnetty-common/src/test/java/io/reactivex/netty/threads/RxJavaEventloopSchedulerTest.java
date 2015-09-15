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

package io.reactivex.netty.threads;

import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.concurrent.TimeUnit;

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
}