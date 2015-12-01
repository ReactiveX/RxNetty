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
package io.reactivex.netty.channel;

/**
 * This event is an indication to atomically replace existing connection input subscriber, if any, with another.
 */
public class ConnectionInputSubscriberReplaceEvent<R, W> {

    private final ConnectionInputSubscriberEvent<R, W> newSubEvent;

    public ConnectionInputSubscriberReplaceEvent(ConnectionInputSubscriberEvent<R, W> newSubEvent) {
        this.newSubEvent = newSubEvent;
    }

    public ConnectionInputSubscriberEvent<R, W> getNewSubEvent() {
        return newSubEvent;
    }
}
