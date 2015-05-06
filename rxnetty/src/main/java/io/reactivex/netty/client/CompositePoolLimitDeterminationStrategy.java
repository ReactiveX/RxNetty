/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.client;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class CompositePoolLimitDeterminationStrategy implements PoolLimitDeterminationStrategy {

    private final PoolLimitDeterminationStrategy[] strategies;

    public CompositePoolLimitDeterminationStrategy(PoolLimitDeterminationStrategy... strategies) {
        if (null == strategies || strategies.length == 0) {
            throw new IllegalArgumentException("Strategies can not be null or empty.");
        }
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            if (null == strategy) {
                throw new IllegalArgumentException("No strategy can be null.");
            }
        }
        this.strategies = strategies;
    }

    @Override
    public boolean acquireCreationPermit(long acquireStartTime, TimeUnit timeUnit) {
        for (int i = 0; i < strategies.length; i++) {
            PoolLimitDeterminationStrategy strategy = strategies[i];
            if (!strategy.acquireCreationPermit(acquireStartTime, timeUnit)) {
                PoolExhaustedException throwable = new PoolExhaustedException();
                if (i > 0) {
                    long now = timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    for (int j = i - 1; j >= 0; j--) {
                        strategies[j].onEvent(ClientMetricsEvent.CONNECT_FAILED, now - acquireStartTime,
                                              timeUnit, throwable,
                                              null); // release all permits acquired before this failure.
                    }
                }
                return false;
            }
        }
        return true; // nothing failed and hence it is OK to create a new connection.
    }

    /**
     * Returns the minimum number of permits available across all strategies.
     *
     * @return The minimum number of permits available across all strategies.
     */
    @Override
    public int getAvailablePermits() {
        int minPermits = Integer.MAX_VALUE;
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            int availablePermits = strategy.getAvailablePermits();
            minPermits = Math.min(minPermits, availablePermits);
        }
        return minPermits; // If will atleast be one strategy (invariant in constructor) and hence this should be the value provided by that strategy.
    }

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            strategy.onEvent(event, duration, timeUnit, throwable, value);
        }
    }

    @Override
    public void onCompleted() {
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            strategy.onCompleted();
        }
    }

    @Override
    public void onSubscribe() {
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            strategy.onCompleted();
        }
    }
}
