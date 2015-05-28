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
package io.reactivex.netty.protocol.http.ws.client;

import rx.Observable;

/**
 * A WebSocket upgrade HTTP request that will generate a {@link WebSocketResponse}
 *
 * @param <O> The type of the content received in the HTTP response, in case, the upgrade was rejected by the server.
 */
public abstract class WebSocketRequest<O> extends Observable<WebSocketResponse<O>> {

    protected WebSocketRequest(OnSubscribe<WebSocketResponse<O>> f) {
        super(f);
    }

    /**
     * Specify any sub protocols that are to be requested to the server as specified by the
     * <a href="https://tools.ietf.org/html/rfc6455#page-12">specifications</a>
     *
     * @param subProtocols Sub protocols to request.
     *
     * @return A new instance of {@link WebSocketRequest} with the sub protocols requested.
     */
    public abstract WebSocketRequest<O> requestSubProtocols(String... subProtocols);

    /**
     * By default, the websocket request made is for the latest version in the specifications, however, if an earlier
     * version is required, it can be updated by this method.
     *
     * @param version WebSocket version.
     *
     * @return A new instance of {@link WebSocketRequest} with the version requested.
     */
    public abstract WebSocketRequest<O> version(int version);
}
