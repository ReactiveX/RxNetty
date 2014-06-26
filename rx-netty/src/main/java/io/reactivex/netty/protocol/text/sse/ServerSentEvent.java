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
package io.reactivex.netty.protocol.text.sse;

/**
 * This class represents a single server-sent event.
 */
public class ServerSentEvent {

    private final String eventId;
    private final String eventType;
    private final String eventData;

    public ServerSentEvent(String eventId, String eventType, String eventData) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Message{");
        sb.append(", eventId='").append(eventId).append('\'');
        sb.append(", eventType='").append(eventType).append('\'');
        sb.append("eventData='").append(eventData).append('\'');
        sb.append('}');
        return sb.toString();
    }
}