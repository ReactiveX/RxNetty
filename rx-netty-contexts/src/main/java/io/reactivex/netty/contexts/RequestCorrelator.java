package io.reactivex.netty.contexts;

import io.netty.channel.ChannelHandlerContext;

/**
 * Passing contexts from inbound request processing to outbound request sending can be very application specific. Some
 * applications may just use a single thread between the inbound and outbound processing and some may spawn off multiple
 * threads to process the request and hence needs to transfer the {@link ContextsContainer} instance from the server
 * processing the request to a client sending an outbound request as part of the original request processing. <br/>
 * Since, this is application specific, this module delegates this particular decision to an implementation of this
 * interface. The actual implementation can choose a medium specific to their application.
 *
 * @author Nitesh Kant
 */
public interface RequestCorrelator extends ContextCapturer {

    /**
     * This does the correlation between an inbound request and all outbound requests that are made during the
     * processing of the same request. <br/>
     *
     * @param context Context for the associated <em>client</em> channel.
     *
     * @return The request id, if exists. {@code null} otherwise.
     */
    String getRequestIdForClientRequest(ChannelHandlerContext context);

    /**
     * This method does the correlation between the inbound {@link ContextsContainer} to a fresh client request.
     *
     * @param requestId Request Id obtained via {@link RequestIdProvider#beforeClientRequest(ChannelHandlerContext)}
     * @param context Context for the associated <em>client</em> channel.
     *
     * @return {@link ContextsContainer} for this request, if any. {@code null} if none exists.
     */
    ContextsContainer getContextForClientRequest(String requestId, ChannelHandlerContext context);

    /**
     * A callback for a fresh request on the underlying channel.
     *
     * @param requestId Request Id for this request.
     * @param contextsContainer Container for this request.
     */
    void onNewServerRequest(String requestId, ContextsContainer contextsContainer);
}
