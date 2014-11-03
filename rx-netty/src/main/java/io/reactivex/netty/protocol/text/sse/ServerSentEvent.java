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
 *
 * @deprecated Use {@link io.reactivex.netty.protocol.http.sse.ServerSentEvent} instead.
 */
@Deprecated
public class ServerSentEvent {

    private final String eventId;
    private final String eventType;
    private final String eventData;
    private final boolean splitMode;

    public ServerSentEvent(String eventId, String eventType, String eventData) {
        this(eventId, eventType, eventData, true);
    }
    public ServerSentEvent(String eventId, String eventType, String eventData, boolean splitMode) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.splitMode = splitMode;
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

    /**
     * By default split mode is enabled, which means that event data string will be split and encoded
     * into multiple data lines if it contains new line characters. It is required for correct data
     * re-assembly on the other side (see http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html#event-stream-interpretation).
     * To improve performance, for data that never include new line characters, the split mode can be turned off.
     * In such case the data string will not be scanned, but will be serialized as is.
     */
    public boolean isSplitMode() {
        return splitMode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Message{");
        sb.append(", eventId='").append(eventId).append('\'');
        sb.append(", eventType='").append(eventType).append('\'');
        sb.append("eventData='").append(eventData).append('\'');
        sb.append("splitMode='").append(splitMode).append('\'');
        sb.append('}');
        return sb.toString();
    }
}