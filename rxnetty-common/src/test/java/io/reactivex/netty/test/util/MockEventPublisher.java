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

import io.reactivex.netty.events.EventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class MockEventPublisher<T extends EventListener> implements EventPublisher, EventSource<T> {

    private static final MockEventPublisher<?> DISABLED_EVENT_PUBLISHER = new MockEventPublisher(true);
    private static final MockEventPublisher<?> ENABLED_EVENT_PUBLISHER = new MockEventPublisher(false);

    private final boolean disable;

    private MockEventPublisher(boolean disable) {
        this.disable = disable;
    }

    public static <T extends EventListener> MockEventPublisher<T> disabled() {
        @SuppressWarnings("unchecked")
        MockEventPublisher<T> t = (MockEventPublisher<T>) DISABLED_EVENT_PUBLISHER;
        return t;
    }

    public static <T extends EventListener> MockEventPublisher<T> enabled() {
        @SuppressWarnings("unchecked")
        MockEventPublisher<T> t = (MockEventPublisher<T>) ENABLED_EVENT_PUBLISHER;
        return t;
    }

    @Override
    public boolean publishingEnabled() {
        return !disable;
    }

    @Override
    public Subscription subscribe(T listener) {
        return Subscriptions.empty();
    }
}
