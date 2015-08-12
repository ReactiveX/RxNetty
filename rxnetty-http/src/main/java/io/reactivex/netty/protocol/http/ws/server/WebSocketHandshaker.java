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
 *
 */
package io.reactivex.netty.protocol.http.ws.server;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.internal.StringUtil;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;
import rx.Subscriber;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
/**
 * The websocket handshaker for sending handshake response back to the client.
 *
 * The defaults chosen by the handshaker can be altered by using the various methods here.
 */
public abstract class WebSocketHandshaker extends Observable<Void> {

    public static final int DEFAULT_MAX_FRAME_PAYLOAD_LENGTH = 65536;
    public static final boolean DEFAULT_ALLOW_EXTENSIONS = true;

    protected WebSocketHandshaker(OnSubscribe<Void> f) {
        super(f);
    }

    public abstract WebSocketHandshaker subprotocol(String... subprotocols);

    public abstract WebSocketHandshaker allowExtensions(boolean allowExtensions);

    public abstract WebSocketHandshaker location(String webSocketLocation);

    public abstract WebSocketHandshaker maxFramePayloadLength(int maxFramePayloadLength);

    public static WebSocketHandshaker newHandshaker(HttpServerRequest<?> request,
                                                    HttpServerResponse<?> upgradeResponse, WebSocketHandler handler) {
        final WebSocketVersion wsVersion = getWsVersion(request);
        return V7to13Handshaker.createNew(wsVersion, request, upgradeResponse, handler);
    }

    public static WebSocketHandshaker newErrorHandshaker(Throwable error) {
        return new ErrorWebSocketHandshaker(error);
    }

    /**
     * <b>This is copied from {@link WebSocketServerHandshaker}</b>
     *
     * Selects the first matching supported sub protocol
     *
     * @param requestedSubprotocols CSV of protocols to be supported. e.g. "chat, superchat"
     * @return First matching supported sub protocol. Null if not found.
     */
    protected static String selectSubprotocol(String requestedSubprotocols, String[] supportedSubProtocols) {
        if (requestedSubprotocols == null || supportedSubProtocols.length == 0) {
            return null;
        }

        String[] requestedSubprotocolArray = StringUtil.split(requestedSubprotocols, ',');

        for (String p: requestedSubprotocolArray) {
            String requestedSubprotocol = p.trim();

            for (String supportedSubprotocol: supportedSubProtocols) {
                if (WebSocketServerHandshaker.SUB_PROTOCOL_WILDCARD.equals(supportedSubprotocol)
                    || requestedSubprotocol.equals(supportedSubprotocol)) {
                    return requestedSubprotocol;
                }
            }
        }

        // No match found
        return null;
    }

    public static boolean isUpgradeRequested(HttpServerRequest<?> upgradeRequest) {
        return null != upgradeRequest && upgradeRequest.containsHeader(HttpHeaderNames.UPGRADE)
                                      && WEBSOCKET.equalsIgnoreCase(upgradeRequest.getHeader(HttpHeaderNames.UPGRADE));
    }

    public static boolean isUpgradeRequested(HttpRequest upgradeRequest) {
        return null != upgradeRequest && upgradeRequest.headers().contains(HttpHeaderNames.UPGRADE)
                                      && WEBSOCKET.equalsIgnoreCase(upgradeRequest.headers()
                                                                                  .get(HttpHeaderNames.UPGRADE));
    }

    private static WebSocketVersion getWsVersion(HttpServerRequest<?> request) {
        String version = request.getHeader(SEC_WEBSOCKET_VERSION);

        switch (version) {
        case "0":
            return WebSocketVersion.V00;
        case "7":
            return WebSocketVersion.V07;
        case "8":
            return WebSocketVersion.V08;
        case "13":
            return WebSocketVersion.V13;
        default:
            return WebSocketVersion.UNKNOWN;
        }
    }

    private static class ErrorWebSocketHandshaker extends WebSocketHandshaker {

        public ErrorWebSocketHandshaker(final Throwable error) {
            super(new OnSubscribe<Void>() {
                @Override
                public void call(Subscriber<? super Void> subscriber) {
                    subscriber.onError(error);
                }
            });
        }

        @Override
        public WebSocketHandshaker subprotocol(String... subprotocols) {
            return this;
        }

        @Override
        public WebSocketHandshaker allowExtensions(boolean allowExtensions) {
            return this;
        }

        @Override
        public WebSocketHandshaker location(String webSocketLocation) {
            return this;
        }

        @Override
        public WebSocketHandshaker maxFramePayloadLength(int maxFramePayloadLength) {
            return this;
        }
    }
}
