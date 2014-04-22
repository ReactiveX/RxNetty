package io.reactivex.netty.contexts.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdGenerator;
import io.reactivex.netty.contexts.RequestIdProvider;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An implementation of {@link RequestIdProvider} for HTTP protocol. <br/>
 * This implementation honors HTTP pipelining and hence maintains a queue of request Ids generated on the same
 * connection. The request Ids from the queue are removed when a response is set in the order they were received. <br/>
 * Since, HTTP pipelining defines that the responses should be sent from the server in the same order they were received,
 * this implementation uses a Queue to store and find request Ids for the responses sent from here.
 *
 * @author Nitesh Kant
 */
public class HttpRequestIdProvider implements RequestIdProvider {

    private static final AttributeKey<ConcurrentLinkedQueue<String>> REQUEST_IDS_KEY =
            AttributeKey.valueOf("rxnetty_http_request_ids_queue");

    private final RequestIdGenerator requestIdGenerator;
    private final RequestCorrelator requestCorrelator;
    private final String requestIdHeaderName;

    public HttpRequestIdProvider(RequestIdGenerator requestIdGenerator, RequestCorrelator requestCorrelator,
                                 String requestIdHeaderName) {
        this.requestIdGenerator = requestIdGenerator;
        this.requestCorrelator = requestCorrelator;
        this.requestIdHeaderName = requestIdHeaderName;
    }

    public HttpRequestIdProvider(String requestIdHeaderName, RequestCorrelator requestCorrelator) {
        this(new RequestIdGenerator() {
            @Override
            public String newRequestId(ContextKeySupplier keySupplier, ChannelHandlerContext context) {
                return UUID.randomUUID().toString();
            }
        }, requestCorrelator, requestIdHeaderName);
    }

    @Override
    public String newRequestId(ContextKeySupplier keySupplier, ChannelHandlerContext context) {
        String requestId = requestIdGenerator.newRequestId(keySupplier, context);
        addRequestId(context, requestId);
        return requestId;
    }

    @Override
    public String onServerRequest(ContextKeySupplier keySupplier, ChannelHandlerContext context) {
        String requestId = keySupplier.getContextValue(requestIdHeaderName);
        if (null != requestId) {
            addRequestId(context, requestId);
        }
        return requestId;
    }

    @Override
    public String beforeServerResponse(ContextKeySupplier responseKeySupplier, ChannelHandlerContext context) {
        return getRequestIdFromQueue(context);
    }

    @Override
    public String beforeClientRequest(ChannelHandlerContext context) {
        String requestId = requestCorrelator.getRequestIdForClientRequest(context);
        if (null != requestId) {
            addRequestId(context, requestId);
        }
        return requestId;
    }

    @Override
    public String onClientResponse(ChannelHandlerContext context) {
        return getRequestIdFromQueue(context);
    }

    private static void addRequestId(ChannelHandlerContext context, String requestId) {
        ConcurrentLinkedQueue<String> requestIdsQueue = context.channel().attr(REQUEST_IDS_KEY).get();
        if (null == requestIdsQueue) {
            requestIdsQueue = new ConcurrentLinkedQueue<String>();
            ConcurrentLinkedQueue<String> existingQueue = context.channel().attr(REQUEST_IDS_KEY).setIfAbsent(requestIdsQueue);
            if (null != existingQueue) {
                requestIdsQueue = existingQueue;
            }
        }
        if (null != requestIdsQueue) {
            requestIdsQueue.offer(requestId); // We can even use add() as this is a lock-free queue. Just using offer() to not confuse folks.
        }
    }

    private static String getRequestIdFromQueue(ChannelHandlerContext context) {
        ConcurrentLinkedQueue<String> requestIdsQueue = context.channel().attr(REQUEST_IDS_KEY).get();
        if (null != requestIdsQueue) {
            return requestIdsQueue.poll(); // Responses should be sent in the same order as the requests were received (HTTP pipelining)
        }
        return null;
    }
}
