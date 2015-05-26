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
package io.reactivex.netty.test.util;

import io.reactivex.netty.protocol.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.protocol.client.PoolLimitDeterminationStrategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MockPoolLimitDeterminationStrategy implements PoolLimitDeterminationStrategy {

    private final MaxConnectionsBasedStrategy delegate;
    private final AtomicInteger acquireCount = new AtomicInteger();
    private final AtomicInteger releaseCount = new AtomicInteger();

    public MockPoolLimitDeterminationStrategy(int maxPermits) {
        delegate = new MaxConnectionsBasedStrategy(maxPermits);
    }

    @Override
    public boolean acquireCreationPermit(long acquireStartTime, TimeUnit timeUnit) {
        acquireCount.incrementAndGet();
        return delegate.acquireCreationPermit(acquireStartTime, timeUnit);
    }

    @Override
    public int getAvailablePermits() {
        return delegate.getAvailablePermits();
    }

    @Override
    public PoolLimitDeterminationStrategy copy() {
        return this;
    }

    @Override
    public void releasePermit() {
        releaseCount.incrementAndGet();
        delegate.releasePermit();
    }

    public int getAcquireCount() {
        return acquireCount.get();
    }

    public int getReleaseCount() {
        return releaseCount.get();
    }
}
