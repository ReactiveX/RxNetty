package io.reactivex.netty.contexts;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class to store {@link ContextsContainer} instances as channel attributes.
 *
 * @author Nitesh Kant
 */
public final class ContextAttributeStorageHelper {

    public static final String CONTAINER_ATTRIBUTE_KEY_NAME = "rxnetty_contexts_container";

    /**
     * Why is this a ConcurrentHashMap?
     * This will always be updated from within a Channel handler, which makes sure that this is always updated by a
     * single thread. However, for protocols which have multiple concurrent requests per connection (SPDY, HTTP2, etc)
     * this may not be true if the requests are potentially processed parallely. So, this is just a safety net.
     */
    public static final AttributeKey<ConcurrentHashMap<String, ContextsContainer>> CONTAINERS_ATTRIBUTE_KEY =
            AttributeKey.valueOf(CONTAINER_ATTRIBUTE_KEY_NAME);

    private ContextAttributeStorageHelper() {
    }

    public static void setContainer(ChannelHandlerContext ctx, String requestId, ContextsContainer container) {
        if (null == ctx) {
            throw new IllegalArgumentException("Context can not be null.");
        }
        if (null == requestId) {
            throw new IllegalArgumentException("Request id can not be null.");
        }
        if (null == container) {
            throw new IllegalArgumentException("Context container can not be null.");
        }
        ConcurrentHashMap<String, ContextsContainer> containers = ctx.channel().attr(CONTAINERS_ATTRIBUTE_KEY).get();
        if (null == containers) {
            containers = new ConcurrentHashMap<String, ContextsContainer>();
            ctx.channel().attr(CONTAINERS_ATTRIBUTE_KEY).set(containers);
        }

        containers.put(requestId, container);
    }

    public static ContextsContainer getContainer(ChannelHandlerContext ctx, String requestId) {
        if (null == ctx) {
            throw new IllegalArgumentException("Context can not be null.");
        }
        if (null == requestId) {
            throw new IllegalArgumentException("Request id can not be null.");
        }
        ConcurrentHashMap<String, ContextsContainer> containers = ctx.channel().attr(CONTAINERS_ATTRIBUTE_KEY).get();
        if (null != containers) {
            return containers.get(requestId);
        }
        return null;
    }
}
