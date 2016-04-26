package io.reactivex.netty.examples;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.mockito.Matchers.*;

public class ExamplesMockLogger {

    private final Logger mock;
    private final Queue<String> logMessages;

    public ExamplesMockLogger(Class<?> exampleClass) {
        mock = Mockito.mock(Logger.class);
        logMessages = new ConcurrentLinkedQueue<>();

        Mockito.doAnswer(new TeeLogsAnswer(logMessages)).when(mock).error(anyString());
        Mockito.doAnswer(new TeeLogsAnswer(logMessages)).when(mock).debug(anyString());
        Mockito.doAnswer(new TeeLogsAnswer(logMessages)).when(mock).info(anyString());
        Mockito.doAnswer(new TeeLogsAnswer(logMessages)).when(mock).warn(anyString());
    }

    public Logger getMock() {
        return mock;
    }

    public Queue<String> getLogMessages() {
        return logMessages;
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
