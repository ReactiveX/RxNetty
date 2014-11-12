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
package io.reactivex.netty.contexts;

import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.DefaultAttributeMap;
import io.reactivex.netty.client.RxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Stack;
import java.util.concurrent.Callable;

/**
 * An implementation of {@link RequestCorrelator} that fetches {@link ContextsContainer} and request Identifer from
 * {@link ThreadLocal} variables. <br/>
 * The usage of this correlator assumes that every {@link EventLoopGroup} used by all {@link RxClient}s in use for
 * the propagation of these contexts, uses {@link ContextAwareEventLoopGroup}. <br/>
 * In case, the application is using any other threadpools, the invariant of the thread state copied from one thread
 * to another is maintained.
 *
 * @author Nitesh Kant
 */
public class ThreadLocalRequestCorrelator implements RequestCorrelator {

    private static final Logger logger = LoggerFactory.getLogger(ThreadLocalRequestCorrelator.class);

    protected static final ThreadLocal<ThreadStateHolder> state = new ThreadLocal<ThreadStateHolder>() {
        @Override
        protected ThreadStateHolder initialValue() {
            return new ThreadStateHolder();
        }
    };

    @Override
    public String getRequestIdForClientRequest() {
        return state.get().getRequestId();
    }

    @Override
    public ContextsContainer getContextForClientRequest(String requestId) {
        return state.get().getContainer();
    }

    @Override
    public void onNewServerRequest(String requestId, ContextsContainer contextsContainer) {
        if (null == requestId) {
            throw new IllegalArgumentException("Request Id can not be null.");
        }
        if (null == contextsContainer) {
            throw new IllegalArgumentException("Context container can not be null.");
        }

        state.get().push(new DefaultAttributeMap());

        state.get().setRequestId(requestId);
        state.get().setContainer(contextsContainer);
    }

    @Override
    public void beforeNewClientRequest(String requestId, ContextsContainer contextsContainer) {
        onNewServerRequest(requestId, contextsContainer); // same effect
    }

    @Override
    public void onClientProcessingEnd(String requestId) {
        state.get().pop();
    }

    @Override
    public void onServerProcessingEnd(String requestId) {
        state.get().pop();
    }

    public static ContextsContainer getCurrentContextContainer() {
        return state.get().getContainer();
    }

    public static String getCurrentRequestId() {
        return state.get().getRequestId();
    }

    @Override
    public <V> Callable<V> makeClosure(final Callable<V> original) {

        final AttributeMap currentState = state.get().peek();

        return new Callable<V>() {
            @Override
            public V call() throws Exception {
                state.get().push(currentState);
                try {
                    return original.call();
                } finally {
                    state.get().pop();
                }
            }
        };
    }

    @Override
    public Runnable makeClosure(final Runnable original) {
        final AttributeMap currentState = state.get().peek();

        return new Runnable() {

            @Override
            public void run() {
                state.get().push(currentState);
                try {
                    original.run();
                } finally {
                    state.get().pop();
                }
            }
        };
    }

    public void dumpThreadState() {
        if (logger.isDebugEnabled()) {
            ThreadStateHolder threadStateHolder = state.get();
            logger.debug("******************** Thread Attributes ********************");
            int count = 0;
            for (AttributeMap threadAttribute : threadStateHolder.threadAttributes) {
                logger.debug("Stack level==> " + ++count);
                logger.debug("Request Id: " + threadAttribute.attr(ThreadStateHolder.requestIdKey));
                logger.debug("Context container: " + threadAttribute.attr(ThreadStateHolder.containerKey));
            }
            logger.debug("***********************************************************");
        }
    }

    public void dumpThreadState(PrintStream outputStream) {
        ThreadStateHolder threadStateHolder = state.get();
        outputStream.println("******************** Thread Attributes ********************");
        int count = 0;
        for (AttributeMap threadAttribute : threadStateHolder.threadAttributes) {
            outputStream.println("Stack level==> " + ++count);
            outputStream.println("Request Id: " + threadAttribute.attr(ThreadStateHolder.requestIdKey));
            outputStream.println("Context container: " + threadAttribute.attr(ThreadStateHolder.containerKey));
        }
        outputStream.println("***********************************************************");
    }

    public static final class ThreadStateHolder {

        private static final AttributeKey<String> requestIdKey =
                AttributeKey.valueOf("rxnetty-contexts-threadlocal-request-id-key");
        private static final AttributeKey<ContextsContainer> containerKey =
                AttributeKey.valueOf("rxnetty-contexts-threadlocal-context-container-key");

        /**
         * This is a stack because the client and server processing can be on the same thread, either by chance or
         * just because there is only a single thread in the system.
         * This means we can not simply set & unset on start & stop of processing as it can step on other's feet.
         * The best way to handle is to use a stack where in states are pushed and popped. If we pop everything, it is
         * as good as clear state.
         * Here {@link AttributeMap} is used just to have a map that stores multiple objects and still is type safe.
         */
        private final Stack<AttributeMap> threadAttributes = new Stack<AttributeMap>();

        public void push(AttributeMap map) {
            threadAttributes.push(map);
        }

        public AttributeMap pop() {
            return threadAttributes.pop();
        }

        public AttributeMap peek() {
            if (isEmpty()) {
                return null;
            }
            return threadAttributes.peek();
        }

        public boolean isEmpty() {
            return threadAttributes.isEmpty();
        }

        public void setRequestId(String requestId) {
            if (!isEmpty()) {
                peek().attr(requestIdKey).set(requestId);
            }
        }

        public void setContainer(ContextsContainer container) {
            if (!isEmpty()) {
                peek().attr(containerKey).set(container);
            }
        }

        public String getRequestId() {
            if (isEmpty()) {
                return null;
            }
            return peek().attr(requestIdKey).get();
        }

        public ContextsContainer getContainer() {
            if (isEmpty()) {
                return null;
            }
            return peek().attr(containerKey).get();
        }
    }
}
