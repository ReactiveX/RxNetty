package io.reactivex.netty.protocol.http.server;

import rx.Observable;
import rx.functions.Func1;

/**
 * Decorator for a {@link RequestHandler} with an accompanying {@link ErrorResponseGenerator}.
 * 
 * @author elandau
 *
 * @param <I>
 * @param <O>
 */
public class RequestHandlerWithErrorMapper<I, O> implements RequestHandler<I, O> {
    private Func1<Throwable, ErrorResponseGenerator<O>> errorMapper;
    private RequestHandler<I, O> handler;
    
    public static <I, O> RequestHandlerWithErrorMapper<I, O> from(RequestHandler<I, O> handler, Func1<Throwable, ErrorResponseGenerator<O>> errorMapper) {
        return new RequestHandlerWithErrorMapper<I, O>(handler, errorMapper);
    }

    public RequestHandlerWithErrorMapper(
            RequestHandler<I, O> handler,
            Func1<Throwable, ErrorResponseGenerator<O>> errorMapper) {
        this.handler = handler;
        this.errorMapper = errorMapper;
    }

    @Override
    public Observable<Void> handle(HttpServerRequest<I> request, final HttpServerResponse<O> response) {
        return handler.handle(request, response)
                .onErrorResumeNext(new Func1<Throwable, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Throwable exception) {
                        // Try to redner the error with the ErrorResponseGenerator
                        ErrorResponseGenerator<O> generator = errorMapper.call(exception);
                        if (generator != null) {
                            generator.updateResponse(response, exception);
                            return Observable.empty();
                        }
                        // Defer the to default error renderer 
                        return Observable.error(exception);
                    }
                });
    }
}
