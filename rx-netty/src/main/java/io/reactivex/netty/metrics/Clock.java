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
package io.reactivex.netty.metrics;

import java.util.concurrent.TimeUnit;

/**
 * A simple utility to wrap start and end times of a call, typically used to give
 * {@link io.reactivex.netty.metrics.MetricEventsListener} callbacks.
 *
 * <h2>Thread Safety</h2>
 *
 * This class is <b>NOT</b> threadsafe.
 *
 * @author Nitesh Kant
 */
public class Clock {

    private final long startTimeMillis = System.currentTimeMillis();
    private long endTimeMillis = -1;
    private long durationMillis = -1;

    /**
     * Stops this clock. This method is idempotent, so, after invoking this method, the duration of the clock is
     * immutable. Hence, you can call this method multiple times with no side-effects.
     *
     * @return The duration in milliseconds for which the clock was running.
     */
    public long stop() {
        if (-1 != endTimeMillis) {
            endTimeMillis = System.currentTimeMillis();
            durationMillis = endTimeMillis - startTimeMillis;
        }
        return durationMillis;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getStartTime(TimeUnit targetUnit) {
        return targetUnit.convert(startTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the duration for which this clock was running in milliseconds.
     *
     * @return The duration for which this clock was running in milliseconds.
     *
     * @throws IllegalStateException If the clock is not yet stopped.
     */
    public long getDurationInMillis() {
        if (isRunning()) {
            throw new IllegalStateException("The clock is not yet stopped.");
        }
        return durationMillis;
    }

    /**
     * Returns the duration for which this clock was running in the given timeunit.
     *
     * @return The duration for which this clock was running in the given timeunit.
     *
     * @throws IllegalStateException If the clock is not yet stopped.
     */
    public long getDuration(TimeUnit targetUnit) {
        if (isRunning()) {
            throw new IllegalStateException("The clock is not yet stopped.");
        }
        return targetUnit.convert(durationMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isRunning() {
        return -1 != durationMillis;
    }
}
