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

import java.util.concurrent.TimeUnit;

public class MockEventListener implements EventListener {

    private int onCompletedCount;
    private int eventInvocationCount;
    private final boolean raiseErrorOnAllInvocations;
    private long duration;
    private TimeUnit timeUnit;
    private Throwable recievedError;
    private String arg;
    private Object customEvent;

    public MockEventListener() {
        this(false);
    }

    public MockEventListener(boolean raiseErrorOnAllInvocations) {
        this.raiseErrorOnAllInvocations = raiseErrorOnAllInvocations;
    }

    public void anEvent() {
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    public void anEventWithArg(String arg) {
        eventInvocationCount++;
        this.arg = arg;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    public void anEventWithDuration(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    public void anEventWithDurationAndError(long duration, TimeUnit timeUnit, Throwable t) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        recievedError = t;
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    public void anEventWithDurationAndArg(long duration, TimeUnit timeUnit, String arg) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.arg = arg;
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    @Override
    public void onCompleted() {
        onCompletedCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    @Override
    public void onCustomEvent(Object event) {
        customEvent = event;
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        customEvent = event;
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    @Override
    public void onCustomEvent(Object event, Throwable throwable) {
        customEvent = event;
        recievedError = throwable;
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        recievedError = throwable;
        customEvent = event;
        eventInvocationCount++;
        if (raiseErrorOnAllInvocations) {
            throw new IllegalStateException("Deliberate exception.");
        }
    }

    public int getOnCompletedCount() {
        return onCompletedCount;
    }

    public int getEventInvocationCount() {
        return eventInvocationCount;
    }

    public long getDuration() {
        return duration;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Throwable getRecievedError() {
        return recievedError;
    }

    public String getArg() {
        return arg;
    }

    public Object getCustomEvent() {
        return customEvent;
    }
}
