package io.reactivex.netty.contexts;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeMap;

/**
 * @author Nitesh Kant
 */
public interface RequestIdGenerator {

    /**
     * Generates a <em>globally unique</em> request identifier.
     *
     * @param keySupplier {@link ContextKeySupplier} for the request.
     * @param channelAttributeMap Channel attribute map, normally obtained as {@link ChannelHandlerContext#channel()}
     *
     * @return The newly generated request id.
     */
    String newRequestId(ContextKeySupplier keySupplier, AttributeMap channelAttributeMap);
}
