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
 * <h2>Memory overhead</h2>
 *
 * One of the major concerns in publishing metric events is the object allocation overhead and having a Clock instance
 * can attribute to such overheads. This is the reason why this class also provides static convenience methods to mark
 * start and end of times to reduce some boiler plate code.
 *
 * @author Nitesh Kant
 */
public class Clock {

    /**
     * The value returned by all static methods in this class, viz.,
     * <ul>
     <li>{@link #newStartTime(TimeUnit)}</li>
     <li>{@link #newStartTimeMillis()}</li>
     <li>{@link #onEndMillis(long)}</li>
     <li>{@link #onEnd(long, TimeUnit)}</li>
     </ul>
     * after calling {@link #disableSystemTimeCalls()}
     */
    public static final long SYSTEM_TIME_DISABLED_TIME = -1;

    private final long startTimeMillis = System.currentTimeMillis();
    private long endTimeMillis = -1;
    private long durationMillis = -1;

    private static volatile boolean disableSystemTimeCalls;

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

    /**
     * An optimization hook for low level benchmarks which will make any subsequent calls to
     * <ul>
     <li>{@link #newStartTime(TimeUnit)}</li>
     <li>{@link #newStartTimeMillis()}</li>
     <li>{@link #onEndMillis(long)}</li>
     <li>{@link #onEnd(long, TimeUnit)}</li>
     </ul>
     * will start returning {@link #SYSTEM_TIME_DISABLED_TIME}. This essentially means that instead of calling
     * {@link System#currentTimeMillis()} these methods will use {@link #SYSTEM_TIME_DISABLED_TIME}
     */
    public static void disableSystemTimeCalls() {
        disableSystemTimeCalls = true;
    }

    public static long newStartTimeMillis() {
        return disableSystemTimeCalls ? SYSTEM_TIME_DISABLED_TIME : System.currentTimeMillis();
    }

    public static long newStartTime(TimeUnit timeUnit) {
        if (disableSystemTimeCalls) {
            return SYSTEM_TIME_DISABLED_TIME;
        }

        if (TimeUnit.MILLISECONDS == timeUnit) {
            return newStartTimeMillis();
        }
        return timeUnit.convert(newStartTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public static long onEnd(long startTime, TimeUnit timeUnit) {
        if (disableSystemTimeCalls) {
            return SYSTEM_TIME_DISABLED_TIME;
        }
        if (TimeUnit.MILLISECONDS == timeUnit) {
            return onEndMillis(startTime);
        }
        long startTimeMillis = TimeUnit.MILLISECONDS.convert(startTime, timeUnit);
        return timeUnit.convert(onEndMillis(startTimeMillis), TimeUnit.MILLISECONDS);
    }

    public static long onEndMillis(long startTimeMillis) {
        if (disableSystemTimeCalls) {
            return SYSTEM_TIME_DISABLED_TIME;
        }
        return System.currentTimeMillis() - startTimeMillis;
    }
}
