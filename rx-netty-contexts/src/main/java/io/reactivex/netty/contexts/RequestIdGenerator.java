package io.reactivex.netty.contexts;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Nitesh Kant
 */
public interface RequestIdGenerator {

    /**
     * Generates a <em>globally unique</em> request identifier.
     *
     * @param keySupplier {@link ContextKeySupplier} for the request.
     * @param context The {@link ChannelHandlerContext} associated with this channel.
     *
     * @return The newly generated request id.
     */
    String newRequestId(ContextKeySupplier keySupplier, ChannelHandlerContext context);
}
