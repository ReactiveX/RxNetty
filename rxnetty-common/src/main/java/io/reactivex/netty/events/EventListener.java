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

import java.util.concurrent.TimeUnit;

/**
 * A listener to subscribe to events published by an {@link EventSource}
 */
public interface EventListener {

    /**
     * Marks the end of all event callbacks. No methods on this listener will ever be called once this method is called.
     */
    void onCompleted();

    /**
     * Typically specific instances on {@link EventListener} will provide events that are fired by RxNetty, however,
     * there may be cases, where a user would want to emit custom events. eg: Creating a custom protocol on top of an
     * existing protocol like TCP, would perhaps require some additional events. In such a case, this callback can be
     * utilized.
     *
     * @param event Event published.
     *
     * @see #onCustomEvent(Object, long, TimeUnit)
     * @see #onCustomEvent(Object, Throwable)
     * @see #onCustomEvent(Object, long, TimeUnit, Throwable)
     */
    void onCustomEvent(Object event);

    /**
     * Typically specific instances on {@link EventListener} will provide events that are fired by RxNetty, however,
     * there may be cases, where a user would want to emit custom events. eg: Creating a custom protocol on top of an
     * existing protocol like TCP, would perhaps require some additional events. In such a case, this callback can be
     * utilized.
     *
     * One should use this overload as opposed to {@link #onCustomEvent(Object)} if the custom event need not be created
     * per invocation but has to be associated with a duration. This is a simple optimization to reduce event creation
     * overhead.
     *
     * @param event Event published.
     * @param duration Duration associated with this event. The semantics of this duration is totally upto the published
     * event.
     * @param timeUnit Timeunit for the duration.
     *
     * @see #onCustomEvent(Object, long, TimeUnit)
     * @see #onCustomEvent(Object, Throwable)
     * @see #onCustomEvent(Object, long, TimeUnit, Throwable)
     */
    void onCustomEvent(Object event, long duration, TimeUnit timeUnit);

    /**
     * Typically specific instances on {@link EventListener} will provide events that are fired by RxNetty, however,
     * there may be cases, where a user would want to emit custom events. eg: Creating a custom protocol on top of an
     * existing protocol like TCP, would perhaps require some additional events. In such a case, this callback can be
     * utilized.
     *
     * One should use this overload as opposed to {@link #onCustomEvent(Object)} if the custom event need not be created
     * per invocation but has to be associated with an error. This is a simple optimization to reduce event creation
     * overhead.
     *
     * @param event Event published.
     *
     * @see #onCustomEvent(Object, long, TimeUnit)
     * @see #onCustomEvent(Object, Throwable)
     * @see #onCustomEvent(Object, long, TimeUnit, Throwable)
     */
    void onCustomEvent(Object event, Throwable throwable);

    /**
     * Typically specific instances on {@link EventListener} will provide events that are fired by RxNetty, however,
     * there may be cases, where a user would want to emit custom events. eg: Creating a custom protocol on top of an
     * existing protocol like TCP, would perhaps require some additional events. In such a case, this callback can be
     * utilized.
     *
     * One should use this overload as opposed to {@link #onCustomEvent(Object)} if the custom event need not be created
     * per invocation but has to be associated with a duration and an error. This is a simple optimization to reduce
     * event creation overhead.
     *
     * @param event Event published.
     * @param duration Duration associated with this event. The semantics of this duration is totally upto the published
     * event.
     * @param timeUnit Timeunit for the duration.
     * @param throwable Error associated with the event.
     *
     * @see #onCustomEvent(Object, long, TimeUnit)
     * @see #onCustomEvent(Object, Throwable)
     * @see #onCustomEvent(Object, long, TimeUnit, Throwable)
     */
    void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable);
}
