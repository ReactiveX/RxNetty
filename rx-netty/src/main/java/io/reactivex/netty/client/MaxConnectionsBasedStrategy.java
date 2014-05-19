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

import io.reactivex.netty.protocol.http.client.CompositeHttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link PoolLimitDeterminationStrategy} that limits the pool based on a maximum connections limit.
 * This limit can be increased or decreased at runtime.
 *
 * @author Nitesh Kant
 */
public class MaxConnectionsBasedStrategy implements CompositeHttpClientBuilder.CloneablePoolLimitDeterminationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MaxConnectionsBasedStrategy.class);

    public static final int DEFAULT_MAX_CONNECTIONS = 1000;

    private final AtomicInteger limitEnforcer;
    private final AtomicInteger maxConnections;
    private final int originalMaxConnLimit; // Used for copy.

    public MaxConnectionsBasedStrategy() {
        this(DEFAULT_MAX_CONNECTIONS);
    }

    public MaxConnectionsBasedStrategy(int maxConnections) {
        this.maxConnections = new AtomicInteger(maxConnections);
        limitEnforcer = new AtomicInteger();
        originalMaxConnLimit = maxConnections;
    }

    @Override
    public boolean acquireCreationPermit() {
        /**
         * As opposed to limitEnforcer.incrementAndGet() we follow this model as this does not change the limitEnforcer
         * value unless there are enough permits.
         * If we were to use incrementAndGet(), in case of overflow (from max allowed limit) we would have to decrement
         * the limitEnforcer. This may show temporary overflows in getMaxConnections() which may be disturbing for a
         * user. However, even if we use incrementAndGet() the counter corrects itself over time.
         * This is just a more semantically correct implementation with similar performance characterstics as
         * incrementAndGet()
         */
        for (;;) {
            final int currentValue = limitEnforcer.get();
            final int newValue = currentValue + 1;
            final int maxAllowedConnections = maxConnections.get();
            if (newValue <= maxAllowedConnections) {
                if (limitEnforcer.compareAndSet(currentValue, newValue)) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public int incrementMaxConnections(int incrementBy) {
        return maxConnections.addAndGet(incrementBy);
    }

    public int decrementMaxConnections(int decrementBy) {
        return maxConnections.addAndGet(-1 * decrementBy);
    }

    public int getMaxConnections() {
        return maxConnections.get();
    }

    @Override
    public int getAvailablePermits() {
        return maxConnections.get() - limitEnforcer.get();
    }

    @Override
    public void onCompleted() {
        // No op.
    }

    @Override
    public void onError(Throwable e) {
        logger.error("Connection pool emitted an error for state change events.", e);
    }

    @Override
    public void onNext(PoolInsightProvider.PoolStateChangeEvent stateChangeEvent) {
        switch (stateChangeEvent) {
            case NewConnectionCreated:
                break;
            case ConnectFailed:
                onConnectFailed();
                break;
            case OnConnectionReuse:
                break;
            case OnConnectionEviction:
                onConnectionEviction();
                break;
            case onAcquireAttempted:
                break;
            case onAcquireSucceeded:
                break;
            case onAcquireFailed:
                break;
            case onReleaseAttempted:
                break;
            case onReleaseSucceeded:
                break;
            case onReleaseFailed:
                break;
        }
    }

    private void onConnectFailed() {
        limitEnforcer.decrementAndGet();
    }

    private void onConnectionEviction() {
        limitEnforcer.decrementAndGet();
    }

    @Override
    public CompositeHttpClientBuilder.CloneablePoolLimitDeterminationStrategy copy() {
        return new MaxConnectionsBasedStrategy(originalMaxConnLimit);
    }
}
