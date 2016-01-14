/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.channel;

/**
 * An extension to {@link AbstractDelegatingConnection} that does not transform read/write types.
 *
 * @param <R> Type of objects read from this connection.
 * @param <W> Type of objects written to this connection.
 */
public abstract class SimpleAbstractDelegatingConnection<R, W> extends AbstractDelegatingConnection<R, W, R, W> {

    protected SimpleAbstractDelegatingConnection(Connection<R, W> delegate) {
        super(delegate);
    }

    protected SimpleAbstractDelegatingConnection(Connection<R, W> delegate, Transformer<W, W> transformer) {
        super(delegate, transformer);
    }
}
