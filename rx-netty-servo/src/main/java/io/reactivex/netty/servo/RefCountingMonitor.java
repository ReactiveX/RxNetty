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

package io.reactivex.netty.servo;

import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsPublisher;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility class that can be used to manage a monitored {@link MetricEventsListener} which is registered with multiple
 * {@link MetricEventsPublisher}s.
 *
 * @author Nitesh Kant
 */
public class RefCountingMonitor {

    protected final String monitorId;
    private final AtomicInteger subscriptionCount = new AtomicInteger();

    public RefCountingMonitor(String monitorId) {
        this.monitorId = monitorId;
    }

    public void onCompleted(Object monitor) {
        if (subscriptionCount.decrementAndGet() == 0) {
            ServoUtils.unregisterObject(monitorId, monitor);
        }
    }

    public void onSubscribe(Object monitor) {
        if (subscriptionCount.getAndIncrement() == 0) {
            ServoUtils.registerObject(monitorId, monitor);
        }
    }
}
