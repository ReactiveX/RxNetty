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
package io.reactivex.netty.protocol.http.ws.client.internal;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.internal.HttpClientRequestImpl;
import io.reactivex.netty.protocol.http.client.internal.RawRequest;
import io.reactivex.netty.protocol.http.ws.client.WebSocketRequest;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import io.reactivex.netty.protocol.http.ws.client.Ws7To13UpgradeHandler;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.reactivex.netty.protocol.http.HttpHandlerNames.*;

public final class WebSocketRequestImpl<O> extends WebSocketRequest<O> {

    private final String[] subProtocolsRequested;
    private final WebSocketVersion version;
    private final HttpClientRequest<?, O> httpRequest;

    private WebSocketRequestImpl(final HttpClientRequest<?, O> httpRequest) {
        this(httpRequest, null, WebSocketVersion.V13);
    }

    private WebSocketRequestImpl(final HttpClientRequest<?, O> httpRequest,  String[] subProtocolsRequested,
                                 WebSocketVersion version) {
        super(new OnSubscribe<WebSocketResponse<O>>() {
            @Override
            public void call(Subscriber<? super WebSocketResponse<O>> subscriber) {
                httpRequest.map(new Func1<HttpClientResponse<O>, WebSocketResponseImpl<O>>() {
                    @Override
                    public WebSocketResponseImpl<O> call(HttpClientResponse<O> response) {
                        return new WebSocketResponseImpl<>(response);
                    }
                }).unsafeSubscribe(subscriber);
            }
        });
        this.httpRequest = httpRequest;
        this.subProtocolsRequested = subProtocolsRequested;
        this.version = version;
    }

    public String[] getSubProtocolsRequested() {
        return subProtocolsRequested;
    }

    @Override
    public WebSocketRequestImpl<O> requestSubProtocols(String... subProtocols) {
        return new WebSocketRequestImpl<>(httpRequest.setHeader(SEC_WEBSOCKET_PROTOCOL,
                                                                expectedSubProtocol(subProtocols)), subProtocols,
                                          version);
    }

    @Override
    public WebSocketRequestImpl<O> version(int version) {
        WebSocketVersion webSocketVersion;

        switch (version) {
        case 7:
            webSocketVersion = WebSocketVersion.V07;
            break;
        case 8:
            webSocketVersion = WebSocketVersion.V08;
            break;
        case 13:
            webSocketVersion = WebSocketVersion.V13;
            break;
        default:
            webSocketVersion = WebSocketVersion.UNKNOWN;
            break;
        }
        return new WebSocketRequestImpl<>(httpRequest.setHeader(SEC_WEBSOCKET_VERSION, version),
                                          subProtocolsRequested, webSocketVersion);
    }

    public static <O> WebSocketRequestImpl<O> createNew(final HttpClientRequestImpl<?, O> httpRequest) {
        /*This makes a copy of the request so that we can safely make modifications to the underlying headers.*/
        @SuppressWarnings("unchecked")
        final HttpClientRequestImpl<?, O> upgradeRequest =
                (HttpClientRequestImpl<?, O>) httpRequest.addChannelHandlerLast(WsClientUpgradeHandler.getName(),
                                                                                new Func0<ChannelHandler>() {
                                                                                    @Override
                                                                                    public ChannelHandler call() {
                                                                                        return new Ws7To13UpgradeHandler();
                                                                                    }
                                                                                })
                                                         .addHeader(HttpHeaderNames.UPGRADE, WEBSOCKET);
        RawRequest<?, O> rawRequest = upgradeRequest.unsafeRawRequest();
        HttpRequest headers = rawRequest.getHeaders();
        headers.headers().add(CONNECTION, HttpHeaderValues.UPGRADE);
        headers.headers().add(SEC_WEBSOCKET_VERSION, WebSocketVersion.V13.toHttpHeaderValue());

        return new WebSocketRequestImpl<>(upgradeRequest);
    }

    private static String expectedSubProtocol(String[] subProtocols) {
        if (null == subProtocols || subProtocols.length == 0) {
            return null;
        }

        if (subProtocols.length == 1) {
            return subProtocols[0];
        }

        StringBuilder builder = new StringBuilder();
        for (String subProtocol : subProtocols) {
            if (builder.length() != 0) {
                builder.append(',');
            }
            builder.append(subProtocol);
        }
        return builder.toString();
    }
}
