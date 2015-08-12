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

package io.reactivex.netty.events;

import io.reactivex.netty.RxNetty;

import java.util.concurrent.TimeUnit;

/**
 * A simple utility to wrap start and end times of a call.
 *
 * <h2>Thread Safety</h2>
 *
 * This class is <b>NOT</b> threadsafe.
 * <h2>Memory overhead</h2>
 *
 * One of the major concerns in publishing events is the object allocation overhead and having a Clock instance
 * can attribute to such overheads. This is the reason why this class also provides static convenience methods to mark
 * start and end of times to reduce some boiler plate code.
 */
public class Clock {

    /**
     * The value returned by all static methods in this class, viz.,
     * <ul>
     <li>{@link #newStartTime(TimeUnit)}</li>
     <li>{@link #newStartTimeNanos()}</li>
     <li>{@link #onEndNanos(long)}</li>
     <li>{@link #onEnd(long, TimeUnit)}</li>
     </ul>
     * after calling {@link RxNetty#disableEventPublishing()}
     */
    public static final long SYSTEM_TIME_DISABLED_TIME = -1;

    private final long startTimeNanos = System.nanoTime();
    private long endTimeNanos = -1;
    private long durationNanos = -1;

    /**
     * Stops this clock. This method is idempotent, so, after invoking this method, the duration of the clock is
     * immutable. Hence, you can call this method multiple times with no side-effects.
     *
     * @return The duration in nanoseconds for which the clock was running.
     */
    public long stop() {
        if (-1 != endTimeNanos) {
            endTimeNanos = System.nanoTime();
            durationNanos = endTimeNanos - startTimeNanos;
        }
        return durationNanos;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public long getStartTime(TimeUnit targetUnit) {
        return targetUnit.convert(startTimeNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the duration for which this clock was running in nanoseconds.
     *
     * @return The duration for which this clock was running in nanoseconds.
     *
     * @throws IllegalStateException If the clock is not yet stopped.
     */
    public long getDurationInNanos() {
        if (isRunning()) {
            throw new IllegalStateException("The clock is not yet stopped.");
        }
        return durationNanos;
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
        return targetUnit.convert(durationNanos, TimeUnit.NANOSECONDS);
    }

    public boolean isRunning() {
        return -1 != durationNanos;
    }

    public static long newStartTimeNanos() {
        return RxNetty.isEventPublishingDisabled() ? SYSTEM_TIME_DISABLED_TIME : System.nanoTime();
    }

    public static long newStartTime(TimeUnit timeUnit) {
        if (RxNetty.isEventPublishingDisabled() ) {
            return SYSTEM_TIME_DISABLED_TIME;
        }

        if (TimeUnit.NANOSECONDS == timeUnit) {
            return newStartTimeNanos();
        }
        return timeUnit.convert(newStartTimeNanos(), TimeUnit.NANOSECONDS);
    }

    public static long onEnd(long startTime, TimeUnit timeUnit) {
        if (RxNetty.isEventPublishingDisabled() ) {
            return SYSTEM_TIME_DISABLED_TIME;
        }
        if (TimeUnit.NANOSECONDS == timeUnit) {
            return onEndNanos(startTime);
        }
        long startTimeNanos = TimeUnit.NANOSECONDS.convert(startTime, timeUnit);
        return timeUnit.convert(onEndNanos(startTimeNanos), TimeUnit.NANOSECONDS);
    }

    public static long onEndNanos(long startTimeNanos) {
        if (RxNetty.isEventPublishingDisabled() ) {
            return SYSTEM_TIME_DISABLED_TIME;
        }
        return System.nanoTime() - startTimeNanos;
    }
}
