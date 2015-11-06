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

import io.reactivex.netty.channel.events.ConnectionEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MockConnectionEventListener extends ConnectionEventListener {

    public enum Event {
        BytesRead, BytesWritten, FlushStart, FlushSuccess, WriteStart, WriteSuccess, WriteFailed, CloseStart,
        CloseSuccess, CloseFailed, CustomEvent, CustomEventWithDuration, CustomEventWithDurationAndError,
        CustomEventWithError, Complete
    }

    private final List<Event> methodsCalled = new ArrayList<>();
    private long bytesRead;
    private long duration;
    private TimeUnit timeUnit;
    private long bytesWritten;
    private Throwable recievedError;
    private Object customeEvent;

    @Override
    public void onByteRead(long bytesRead) {
        methodsCalled.add(Event.BytesRead);
        this.bytesRead = bytesRead;
    }

    @Override
    public void onByteWritten(long bytesWritten) {
        methodsCalled.add(Event.BytesWritten);
        this.bytesWritten = bytesWritten;
    }

    @Override
    public void onFlushStart() {
        methodsCalled.add(Event.FlushStart);
    }

    @Override
    public void onFlushComplete(long duration, TimeUnit timeUnit) {
        methodsCalled.add(Event.FlushSuccess);
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    @Override
    public void onWriteStart() {
        methodsCalled.add(Event.WriteStart);
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit) {
        methodsCalled.add(Event.WriteSuccess);
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        methodsCalled.add(Event.WriteFailed);
        this.duration = duration;
        this.timeUnit = timeUnit;
        recievedError = throwable;
    }

    @Override
    public void onConnectionCloseStart() {
        methodsCalled.add(Event.CloseStart);
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        methodsCalled.add(Event.CloseSuccess);
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        methodsCalled.add(Event.CloseFailed);
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.recievedError = recievedError;
    }

    @Override
    public void onCustomEvent(Object event) {
        methodsCalled.add(Event.CustomEvent);
        customeEvent = event;
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
        methodsCalled.add(Event.CustomEventWithDuration);
        customeEvent = event;
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable) {
        methodsCalled.add(Event.CustomEventWithDurationAndError);
        customeEvent = event;
        this.duration = duration;
        this.timeUnit = timeUnit;
        recievedError = throwable;
    }

    @Override
    public void onCustomEvent(Object event, Throwable throwable) {
        methodsCalled.add(Event.CustomEventWithError);
        customeEvent = event;
        recievedError = throwable;
    }

    @Override
    public void onCompleted() {
        methodsCalled.add(Event.Complete);
    }

    public void assertMethodsCalled(Event... events) {
        if (methodsCalled.size() != events.length) {
            throw new AssertionError("Unexpected methods called count. Methods called: " + methodsCalled.size()
                                     + ". Expected: " + events.length);
        }

        if (methodsCalled.containsAll(Arrays.asList(events))) {
            throw new AssertionError("Unexpected methods called count. Methods called: " + methodsCalled
                                     + ". Expected: " + Arrays.toString(events));
        }
    }

    public List<Event> getMethodsCalled() {
        return methodsCalled;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public long getDuration() {
        return duration;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public Throwable getRecievedError() {
        return recievedError;
    }

    public Object getCustomEvent() {
        return customeEvent;
    }
}
