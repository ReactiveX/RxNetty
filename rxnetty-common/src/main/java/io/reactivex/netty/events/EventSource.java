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

import rx.Subscription;

/**
 * An event source to which {@link EventListener}s can subscribe to receive events.
 */
public interface EventSource<T extends EventListener> {

    /**
     * Subscribes the passed {@code listener} for events published by this source.
     *
     * @param listener Listener for events published by this source.
     *
     * @return Subscription, from which one can unsubscribe to stop receiving events.
     */
    Subscription subscribe(T listener);
}
