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
 * A contract to listen for events based on which all metrics can be calculated.
 *
 * <h2>Multiple Subscriptions</h2>
 *
 * Theoretically one can register the same listener instance to multiple {@link MetricEventsPublisher}s, however in that
 * case, there will be multiple callbacks to {@link #onSubscribe()} and {@link #onCompleted()}
 *
 * @author Nitesh Kant
 *
 * @param <E> The type of {@link MetricsEvent} this listener listens.
 *
 */
public interface MetricEventsListener<E extends MetricsEvent<?>> {

    int NO_DURATION = -1;
    Object NO_VALUE = null;
    Throwable NO_ERROR = null;
    TimeUnit NO_TIME_UNIT = null;

    /**
     * Event callback for any {@link MetricsEvent}. The parameters passed are all the contextual information possible for
     * any event. There presence or absence will depend on the type of event.
     *
     * @param event Event for which this callback has been invoked. This will never be {@code null}
     * @param duration If the passed event is {@link MetricsEvent#isTimed()} then the actual duration, else
     * {@link #NO_DURATION}
     * @param timeUnit The time unit for the duration, if exists, else {@link #NO_TIME_UNIT}
     * @param throwable If the passed event is {@link MetricsEvent#isError()} then the cause of the error, else
     * {@link #NO_ERROR}
     * @param value If the passed event requires custom object to be passed, then that object, else {@link #NO_VALUE}
     */
    void onEvent(E event, long duration, TimeUnit timeUnit, Throwable throwable, Object value);

    /**
     * Marks the end of all event callbacks. No methods on this listener will ever be called once this method is called.
     */
    void onCompleted();

    /**
     * A callback when this listener is subscribed to a {@link MetricEventsPublisher}.
     */
    void onSubscribe();
}
