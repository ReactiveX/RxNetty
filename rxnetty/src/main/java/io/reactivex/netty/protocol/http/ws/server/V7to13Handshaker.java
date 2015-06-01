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

import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.ws.server.Ws7To13UpgradeHandler.WebSocket7To13UpgradeAcceptedEvent;
import rx.Subscriber;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * Implementation of {@link WebSocketHandshaker} for web socket spec versions 7.0 to 13.0 (includes final RFC)
 */
final class V7to13Handshaker extends WebSocketHandshaker {

    private final State state;
    private final HttpServerRequest<?> request;
    private final WebSocketHandler handler;

    private V7to13Handshaker(final State state, final HttpServerRequest<?> request, final WebSocketHandler handler) {
        super(new OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                state.upgradeResponse.unsafeNettyChannel()
                                     .pipeline()
                                     .fireUserEventTriggered(new WebSocket7To13UpgradeAcceptedEvent(subscriber, handler,
                                                                                                    state, request));
            }
        });
        this.state = state;
        this.request = request;
        this.handler = handler;
    }

    @Override
    public WebSocketHandshaker subprotocol(String... subprotocols) {
        return new V7to13Handshaker(new State(state, subprotocols), request, handler);
    }

    @Override
    public WebSocketHandshaker allowExtensions(boolean allowExtensions) {
        return new V7to13Handshaker(new State(state, allowExtensions), request, handler);
    }

    @Override
    public WebSocketHandshaker location(String webSocketLocation) {
        return new V7to13Handshaker(new State(state, webSocketLocation), request, handler);
    }

    @Override
    public WebSocketHandshaker maxFramePayloadLength(int maxFramePayloadLength) {
        return new V7to13Handshaker(new State(state, maxFramePayloadLength), request, handler);
    }

    static V7to13Handshaker createNew(WebSocketVersion version, HttpServerRequest<?> request,
                                      HttpServerResponse<?> upgradeResponse, WebSocketHandler handler) {
        return new V7to13Handshaker(new State(version, request, upgradeResponse), request, handler);
    }

    /*package-private, used by upgrade handler*/static final class State {

        private final WebSocketVersion version;
        private final HttpServerResponse<?> upgradeResponse;
        private final String[] supportedSubProtocols;
        private final String locationForV00;
        private final boolean allowExtensions;
        private final int maxFramePayloadLength;
        private final String secWSkey;
        private final String requestSubProtocols;

        private State(WebSocketVersion version, HttpServerRequest<?> request, HttpServerResponse<?> upgradeResponse) {
            this(getKey(request), getRequestedProtocols(request), version, upgradeResponse, null, null,
                 DEFAULT_ALLOW_EXTENSIONS, DEFAULT_MAX_FRAME_PAYLOAD_LENGTH);
        }

        private State(State current, String... subprotocols) {
            this(current.secWSkey, current.requestSubProtocols, current.version, current.upgradeResponse,
                 subprotocols, current.locationForV00, current.allowExtensions, current.maxFramePayloadLength);
        }

        private State(State current, int maxFramePayloadLength) {
            this(current.secWSkey, current.requestSubProtocols, current.version, current.upgradeResponse,
                 current.supportedSubProtocols, current.locationForV00, current.allowExtensions,
                 maxFramePayloadLength);
        }

        private State(State current, boolean allowExtensions) {
            this(current.secWSkey, current.requestSubProtocols, current.version, current.upgradeResponse,
                 current.supportedSubProtocols, current.locationForV00, allowExtensions,
                 current.maxFramePayloadLength);
        }

        private State(String secWSkey, String requestSubProtocols, WebSocketVersion version,
                      HttpServerResponse<?> upgradeResponse, String[] supportedSubProtocols, String locationForV00,
                      boolean allowExtensions, int maxFramePayloadLength) {
            this.secWSkey = secWSkey;
            this.requestSubProtocols = requestSubProtocols;
            this.version = version;
            this.upgradeResponse = upgradeResponse;
            this.supportedSubProtocols = supportedSubProtocols;
            this.locationForV00 = locationForV00;
            this.allowExtensions = allowExtensions;
            this.maxFramePayloadLength = maxFramePayloadLength;
        }

        private static String getRequestedProtocols(HttpServerRequest<?> request) {
            return request.getHeader(SEC_WEBSOCKET_PROTOCOL);
        }

        private static String getKey(HttpServerRequest<?> request) {
            return request.getHeader(SEC_WEBSOCKET_KEY);
        }

        public WebSocketVersion getVersion() {
            return version;
        }

        public HttpServerResponse<?> getUpgradeResponse() {
            return upgradeResponse;
        }

        public String[] getSupportedSubProtocols() {
            return supportedSubProtocols;
        }

        public String getLocationForV00() {
            return locationForV00;
        }

        public boolean isAllowExtensions() {
            return allowExtensions;
        }

        public int getMaxFramePayloadLength() {
            return maxFramePayloadLength;
        }

        public String getSecWSkey() {
            return secWSkey;
        }

        public String getRequestSubProtocols() {
            return requestSubProtocols;
        }
    }
}
