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
package io.reactivex.netty.util;

import io.netty.channel.ChannelHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import rx.functions.Func0;

import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link LoggingHandler} is a shaerable handler and hence need not be created for all channels. This factory
 * manages a static map of log level -> instance which can be used directly instead of creating a new factory per
 * client.
 */
public class LoggingHandlerFactory implements Func0<ChannelHandler> {

    private static final ConcurrentMap<String, EnumMap<LogLevel, LoggingHandlerFactory>> factories =
            new ConcurrentHashMap<>();

    private final LoggingHandler loggingHandler;

    private LoggingHandlerFactory(String name, LogLevel wireLoggingLevel) {
        loggingHandler = new LoggingHandler(name, wireLoggingLevel);
    }

    public static LoggingHandler get(String name, LogLevel logLevel) {
        return getFactory(name, logLevel).loggingHandler;
    }

    public static LoggingHandlerFactory getFactory(String name, LogLevel logLevel) {
        EnumMap<LogLevel, LoggingHandlerFactory> f = factories.get(name);
        if (null == f) {
            f = newEnumMap(name);
            EnumMap<LogLevel, LoggingHandlerFactory> existing = factories.putIfAbsent(name, f);
            if (null != existing) {
                f = existing;
            }
        }
        return f.get(logLevel);
    }

    @Override
    public ChannelHandler call() {
        return loggingHandler;/*logging handler is shareable.*/
    }

    private static EnumMap<LogLevel, LoggingHandlerFactory> newEnumMap(String name) {
        EnumMap<LogLevel, LoggingHandlerFactory> toReturn = new EnumMap<>(LogLevel.class);
        for (LogLevel logLevel : LogLevel.values()) {
            toReturn.put(logLevel, new LoggingHandlerFactory(name, logLevel));
        }
        return toReturn;
    }
}
