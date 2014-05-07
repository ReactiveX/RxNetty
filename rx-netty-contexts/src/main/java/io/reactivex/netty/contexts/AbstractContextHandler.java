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
