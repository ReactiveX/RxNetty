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

import io.netty.channel.ChannelDuplexHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <R> The type of object this handler will read.
 * @param <W> The type of object this handler will write.
 *
 * @author Nitesh Kant
 */
public abstract class AbstractContextHandler<R, W> extends ChannelDuplexHandler {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractContextHandler.class);

    /**
     * Asserts whether the passed message is an acceptable object type to read. If it is not, this handler will just pass
     * this message further in the pipeline.
     *
     * @param msg Message that is being read.
     *
     * @return {@code true} if the message is acceptable.
     */
    protected abstract boolean isAcceptableToRead(Object msg);

    /**
     * Asserts whether the passed message is an acceptable object type to write. If it is not, this handler will just pass
     * this message further in the pipeline.
     *
     * @param msg Message that is being written.
     *
     * @return {@code true} if the message is acceptable.
     */
    protected abstract boolean isAcceptableToWrite(Object msg);

    /**
     * Adds a key to the message that is written.
     *
     * @param msg Message that is being written.
     * @param key Key name to add.
     * @param value Key value to add.
     */
    protected abstract void addKey(W msg, String key, String value);

    /**
     * Creates a new {@link ContextKeySupplier} for the passed message to be written.
     *
     * @param msg Message to be written.
     *
     * @return The newly created {@link ContextKeySupplier}
     */
    protected abstract ContextKeySupplier newKeySupplierForWrite(W msg);

    /**
     * Creates a new {@link ContextKeySupplier} for the passed message that is being read.
     *
     * @param msg Message that is being read.
     *
     * @return The newly created {@link ContextKeySupplier}
     */
    protected abstract ContextKeySupplier newKeySupplierForRead(R msg);
}
