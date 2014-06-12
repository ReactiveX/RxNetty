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
package io.reactivex.netty.servo.tcp;

import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricsEvent;
import io.reactivex.netty.servo.ServoUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nitesh Kant
 */
public abstract class AbstractShareableListener<E extends MetricsEvent<?>> implements MetricEventsListener<E> {

    protected final String monitorId;
    private final AtomicInteger subscriptionCount = new AtomicInteger();

    protected AbstractShareableListener(String monitorId) {
        this.monitorId = monitorId;
    }

    @Override
    public void onCompleted() {
        if (subscriptionCount.decrementAndGet() == 0) {
            ServoUtils.unregisterObject(monitorId, this);
        }
    }

    @Override
    public void onSubscribe() {
        if (subscriptionCount.incrementAndGet() == 0) {
            ServoUtils.registerObject(monitorId, this);
        }
    }
}
