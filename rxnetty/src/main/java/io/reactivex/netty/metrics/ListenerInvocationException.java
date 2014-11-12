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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class ListenerInvocationException extends RuntimeException {

    private Map<MetricEventsListener<?>, Throwable> exceptions;
    private String message;

    private static final long serialVersionUID = -4381062024201397997L;

    @SuppressWarnings("rawtypes")
    protected ListenerInvocationException() {
        super("Metric event listener invocation failed.");
        exceptions = new HashMap<MetricEventsListener<?>, Throwable>();
        message = super.getMessage();
    }

    protected void addException(MetricEventsListener<?> listener, Throwable error) {
        exceptions.put(listener, error);
    }

    protected void finish() {
        exceptions = Collections.unmodifiableMap(exceptions);
        StringBuilder msgBuilder = new StringBuilder(getMessage()).append(". Errors: \n");
        for (Map.Entry<MetricEventsListener<?>, Throwable> exceptionEntry : exceptions.entrySet()) {
            msgBuilder.append("Listener: ");
            msgBuilder.append(exceptionEntry.getKey().getClass().getSimpleName());
            msgBuilder.append("\n Error:");
            ByteArrayOutputStream stackTraceStream = new ByteArrayOutputStream();
            exceptionEntry.getValue().printStackTrace(new PrintStream(stackTraceStream));
            msgBuilder.append(stackTraceStream.toString());
        }
        message = msgBuilder.toString();
    }

    public Map<MetricEventsListener<?>, Throwable> getExceptions() {
        return exceptions;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
