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
package io.reactivex.netty.events;

/**
 * A contract for any publisher of events.
 */
public interface EventPublisher {

    /**
     * Returns {@code true} if event publishing is enabled. This is primarily used to short-circuit event publishing
     * if the publishing is not enabled. Event publishing will be disabled if there are no active listeners or has
     * been explicitly disabled using {@link io.reactivex.netty.RxNetty#disableEventPublishing()}
     *
     * @return {@code true} if event publishing is enabled.
     */
    boolean publishingEnabled();
}
