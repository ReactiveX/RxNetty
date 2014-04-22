package io.reactivex.netty.contexts;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.reactivex.netty.client.RxClient;

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

    private static final ThreadLocal<String> requestId = new ThreadLocal<String>();
    private static final ThreadLocal<ContextsContainer> contextContainer = new ThreadLocal<ContextsContainer>();

    @Override
    public String getRequestIdForClientRequest(ChannelHandlerContext context) {
        return requestId.get();
    }

    @Override
    public ContextsContainer getContextForClientRequest(String requestId, ChannelHandlerContext context) {
        return contextContainer.get();
    }

    @Override
    public void onNewServerRequest(String requestId, ContextsContainer contextsContainer) {
        if (null == requestId) {
            throw new IllegalArgumentException("Request Id can not be null.");
        }
        if (null == contextsContainer) {
            throw new IllegalArgumentException("Context container can not be null.");
        }
        ThreadLocalRequestCorrelator.requestId.set(requestId);
        contextContainer.set(contextsContainer);
    }

    @Override
    public <V> Callable<V> makeClosure(final Callable<V> original) {
        final String calleeRequestId = requestId.get();
        final ContextsContainer calleeContainer = contextContainer.get();

        return new Callable<V>() {
            @Override
            public V call() throws Exception {
                final String originalRequestId = requestId.get();
                final ContextsContainer originalContainer = contextContainer.get();

                try {
                    requestId.set(calleeRequestId);
                    contextContainer.set(calleeContainer);
                    return original.call();
                } finally {
                    requestId.set(originalRequestId);
                    contextContainer.set(originalContainer);
                }
            }
        };
    }

    @Override
    public Runnable makeClosure(final Runnable original) {
        final String calleeRequestId = requestId.get();
        final ContextsContainer calleeContainer = contextContainer.get();

        return new Runnable() {

            @Override
            public void run() {
                final String originalRequestId = requestId.get();
                final ContextsContainer originalContainer = contextContainer.get();

                try {
                    requestId.set(calleeRequestId);
                    contextContainer.set(calleeContainer);
                    original.run();
                } finally {
                    requestId.set(originalRequestId);
                    contextContainer.set(originalContainer);
                }
            }
        };
    }
}
