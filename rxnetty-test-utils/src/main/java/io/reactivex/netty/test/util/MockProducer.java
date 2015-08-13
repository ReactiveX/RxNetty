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
package io.reactivex.netty.test.util;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import rx.Producer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MockProducer implements Producer {

    private final AtomicLong requested = new AtomicLong();
    private final AtomicInteger negativeRequestCount = new AtomicInteger();
    private final AtomicInteger maxValueRequestedCount = new AtomicInteger();

    @Override
    public void request(long n) {
        if (Long.MAX_VALUE == n) {
            requested.set(Long.MAX_VALUE);
            maxValueRequestedCount.incrementAndGet();
        } else if (n <= 0) {
            negativeRequestCount.incrementAndGet();
        }
        requested.addAndGet(n);
    }

    public long getRequested() {
        return requested.get();
    }

    public void assertIllegalRequest() {
        final int negReqCnt = negativeRequestCount.get();
        MatcherAssert.assertThat("Negative items requested " + negReqCnt + " times.", negReqCnt, Matchers.is(0));
    }

    public void assertBackpressureRequested() {
        final int maxValReqCnt = maxValueRequestedCount.get();
        MatcherAssert.assertThat("Backpressure disabled " + maxValReqCnt + " times.", maxValReqCnt, Matchers.is(0));
    }

    public void reset() {
        requested.set(0);
        negativeRequestCount.set(0);
    }
}
