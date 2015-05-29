/*
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.netty.protocol.http.ws.server;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

/**
 * A utility to provide convenience {@link RequestHandler} implementations for Web Sockets.
 */
public final class WebSocketHandlers {

    private WebSocketHandlers() {
    }

    /**
     * Returns a {@link RequestHandler} that accepts all WebSocket upgrade requests by delegating it to the passed
     * handler but sends an HTTP 404 response for all other requests.
     *
     * @param handler Web Socket handler for all web socket upgrade requests.
     *
     * @return request handler.
     */
    public static <I, O> RequestHandler<I, O> acceptAllUpgrades(final WebSocketHandler handler) {
        return new RequestHandler<I, O>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<I> request, HttpServerResponse<O> response) {
                if (request.isWebSocketUpgradeRequested()) {
                    return response.acceptWebSocketUpgrade(handler);
                }

                return response.setStatus(HttpResponseStatus.NOT_FOUND)
                               .write(Observable.<O>empty());
            }
        };
    }
}
