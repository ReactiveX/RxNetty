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
 */
package io.reactivex.netty.examples;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.mockito.Matchers.*;

public final class ExamplesTestUtil {

    private ExamplesTestUtil() {
    }

    public static Queue<String> setupServerLogger(Class<? extends AbstractServerExample> mainClass)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return _setupLogger(mainClass);
    }

    public static Queue<String> setupClientLogger(Class<? extends AbstractClientExample> mainClass)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return _setupLogger(mainClass);
    }

    private static Queue<String> _setupLogger(Class<?> mainClass)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Logger loggerMock = Mockito.mock(Logger.class);
        final Queue<String> output = new ConcurrentLinkedQueue<>();

        Mockito.doAnswer(new TeeLogsAnswer(output)).when(loggerMock).error(anyString());
        Mockito.doAnswer(new TeeLogsAnswer(output)).when(loggerMock).debug(anyString());
        Mockito.doAnswer(new TeeLogsAnswer(output)).when(loggerMock).info(anyString());
        Mockito.doAnswer(new TeeLogsAnswer(output)).when(loggerMock).warn(anyString());

        final Method mockLoggerMethod = mainClass.getMethod("mockLogger", Logger.class);
        mockLoggerMethod.invoke(null, loggerMock);
        return output;
    }

    private static class TeeLogsAnswer implements Answer<Void> {
        private final Queue<String> output;

        public TeeLogsAnswer(Queue<String> output) {
            this.output = output;
        }

        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            String logged = (String) invocation.getArguments()[0];
            System.out.println(logged);
            output.add(logged);
            return null;
        }
    }
}
