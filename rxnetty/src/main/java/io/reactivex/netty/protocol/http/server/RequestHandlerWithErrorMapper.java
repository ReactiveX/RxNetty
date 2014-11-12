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
