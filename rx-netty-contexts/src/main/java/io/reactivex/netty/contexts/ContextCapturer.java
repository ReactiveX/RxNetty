package io.reactivex.netty.contexts;

import io.netty.util.Attribute;

import java.util.concurrent.Callable;

/**
 * A contract for passing {@link ContextsContainer} objects between threads. This is specifically useful to pass these
 * objects between an inbound and outbound channel during request processing. <br/>
 * Once, the {@link ContextsContainer} objects is attached to a channel, via {@link Attribute}, it is recommended to
 * pick it up from there instead of worrying about the threads on which it is put. <br/>
 *
 */
public interface ContextCapturer {

    /**
     * Encloses the passed {@link Callable} into a closure so that it can capture the contexts as it exists when this
     * {@link Callable} is created and use it during the context's execution, which happens in a different thread.
     *
     * @param original Original {@link Callable} to be invoked from the closure.
     * @param <V> Return type of the {@link Callable}
     *
     * @return The closure enclosing the original {@link Callable}
     */
    <V> Callable<V> makeClosure(Callable<V> original);

    /**
     * Encloses the passed {@link Runnable} into a closure so that it can capture the contexts as it exists when this
     * {@link Runnable} is created and use it during the context's execution, which happens in a different thread.
     *
     * @param original Original {@link Runnable} to be invoked from the closure.
     *
     * @return The closure enclosing the original {@link Runnable}
     */
    Runnable makeClosure(Runnable original);
}
