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
package io.reactivex.netty.codec;

/**
 * A list of all handler names added by the framework. This is just to ensure consistency in naming.
 */
public enum HandlerNames {

    WireLogging("wire-logging-handler"),
    PrimitiveConverter("primitive-converter"),
    SslHandler("ssl-handler"),
    SslConnectionEmissionHandler("ssl-connection-emitter"),
    ClientReadTimeoutHandler("client-read-timeout-handler"),
    HttpClientCodec("http-client-codec"),
    HttpServerDecoder("http-server-request-decoder"),
    HttpServerEncoder("http-server-response-encoder"),
    WsServerDecoder("ws-server-request-decoder"),
    WsServerEncoder("ws-server-response-encoder"),
    WsServerUpgradeHandler("ws-server-upgrade-handler"),
    WsClientDecoder("ws-client-request-decoder"),
    WsClientEncoder("ws-client-response-encoder"),
    WsClientUpgradeHandler("ws-client-upgrade-handler"),
    SseClientCodec("sse-client-codec"),
    SseServerCodec("sse-server-codec"),
    ;

    private final String name;

    HandlerNames(String name) {
        this.name = qualify(name);
    }

    public String getName() {
        return name;
    }

    private static String qualify(String name) {
        return "_rx_netty_" + name;

    }
}
