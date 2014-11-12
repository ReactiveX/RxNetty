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
