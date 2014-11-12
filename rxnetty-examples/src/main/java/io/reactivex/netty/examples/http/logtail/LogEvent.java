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

package io.reactivex.netty.examples.http.logtail;

import java.util.regex.Pattern;

/**
 * @author Tomasz Bak
 */
public class LogEvent {


    private static final Pattern COMMA = Pattern.compile(",");

    public enum LogLevel {
        ERROR,
        INFO,
        DEBUG
    }

    private final long timeStamp;
    private final String source;
    private final LogLevel level;
    private final String message;

    public LogEvent(long timeStamp, String source, LogLevel level, String message) {
        this.timeStamp = timeStamp;
        this.source = source;
        this.level = level;
        this.message = message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getSource() {
        return source;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String toCSV() {
        return timeStamp + "," + source + ',' + level.name() + ',' + message;
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "timeStamp=" + timeStamp +
                ", source='" + source + '\'' +
                ", level=" + level +
                ", message='" + message + '\'' +
                '}';
    }

    public static LogEvent fromCSV(String csvLine) {
        String[] parts = COMMA.split(csvLine.trim());
        return new LogEvent(Long.valueOf(parts[0]), parts[1], LogLevel.valueOf(parts[2]), parts[3]);
    }

    public static LogEvent randomLogEvent(String source) {
        return new LogEvent(
                System.currentTimeMillis(),
                source,
                LogLevel.values()[(int) (Math.random() * LogLevel.values().length)],
                "rx-netty sse example"
        );
    }
}
