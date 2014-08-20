package io.reactivex.netty.protocol.http.server.file;

import rx.Observable;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

/**
 * Encapsulate a request handler and the URLResolver.  Used in {@link MultipleFileRequesthandler}
 * 
 * @author elandau
 *
 * @param <I>
 * @param <O>
 */
public class OwnableRequestHandler<I, O> implements RequestHandler<I, O> {

    private final RequestHandler<I, O> delegate;
    private final URIResolver resolver;
    
    public OwnableRequestHandler(URIResolver resolver, RequestHandler<I, O> delegate) {
        this.delegate = delegate;
        this.resolver = resolver;
    }
    
    /**
     * @param request
     * @return Returns true if the RequestHandler owns the file resolved from this URI
     */
    public boolean owns(HttpServerRequest<I> request) {
        return null != resolver.getUri(request.getUri());
    }
    
    @Override
    public Observable<Void> handle(HttpServerRequest<I> request, HttpServerResponse<O> response) {
        return delegate.handle(request, response);
    }
}
