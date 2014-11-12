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

/**
 * An interface usually implemented as an {@link Enum} representing a metric event.
 */
@SuppressWarnings("rawtypes")
public interface MetricsEvent<T extends Enum> {

    T getType();

    boolean isTimed();

    boolean isError();

    /**
     * This interface is a "best-practice" rather than a contract as a more strongly required contract is for the event
     * type to be an enum.
     */
    interface MetricEventType {

        boolean isTimed();

        boolean isError();

        Class<?> getOptionalDataType();
    }
}
