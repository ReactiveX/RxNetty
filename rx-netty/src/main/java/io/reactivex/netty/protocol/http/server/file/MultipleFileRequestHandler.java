package io.reactivex.netty.protocol.http.server.file;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

import java.util.Collection;
import java.util.List;

import rx.Observable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Serve files from multiple sources.
 * 
 * For example, serve files from the class path and if not found look for files in a specific
 * folder.  Can also be used proxy files from a remote server.
 * 
 * @author elandau
 */
public class MultipleFileRequestHandler implements RequestHandler<ByteBuf, ByteBuf>{

    /**
     * Simple Builder for creating a MultipleFileRequestHandler 
     * @author elandau
     *
     */
    public static class Builder {
        private List<OwnableRequestHandler<ByteBuf, ByteBuf>> handlers = Lists.newArrayList();
        public Builder add(URIResolver resolver, RequestHandler<ByteBuf, ByteBuf> handler) {
            handlers.add(new OwnableRequestHandler<ByteBuf, ByteBuf>(resolver, handler));
            return this;
        }
        
        public MultipleFileRequestHandler build() {
            return new MultipleFileRequestHandler(ImmutableList.copyOf(handlers));
        }
    }

    private final Collection<OwnableRequestHandler<ByteBuf, ByteBuf>> handlers;
    
    public MultipleFileRequestHandler(Collection<OwnableRequestHandler<ByteBuf, ByteBuf>> handlers) {
        this.handlers = handlers;
    }
    
    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        // Iterate through handlers in the order in which they were configured
        // If no handler found then respond with a 404
        for (OwnableRequestHandler<ByteBuf, ByteBuf> handler : handlers) {
            if (handler.owns(request)) {
                return handler.handle(request, response);
            }
        }
        return FileResponses.sendError(response, HttpResponseStatus.NOT_FOUND);
    }

}
