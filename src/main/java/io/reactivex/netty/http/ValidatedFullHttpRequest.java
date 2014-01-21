/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;

/**
 * A wrapper of {@link DefaultFullHttpRequest} that validates given URL, as well as sets
 * up necessary header information
 */
public class ValidatedFullHttpRequest extends DefaultFullHttpRequest {
    private final UriInfo uriInfo;

    public static ValidatedFullHttpRequest get(URI uri) {
        return get(uri.toString());
    }

    public static ValidatedFullHttpRequest get(String uri) {
        return new ValidatedFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    }

    public static ValidatedFullHttpRequest post(String uri, ByteBuf content) {
        return new ValidatedFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri, content);
    }

    public static ValidatedFullHttpRequest post(URI uri, ByteBuf content) {
        return post(uri.toString(), content);
    }

    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
        this.uriInfo = UriInfo.fromUri(uri);

        init();
    }

    public ValidatedFullHttpRequest(HttpVersion version, HttpMethod method, URI uri) {
        this(version, method, uri.toString());
    }

    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content) {
        super(httpVersion, method, uri, content);
        this.uriInfo = UriInfo.fromUri(uri);

        init();
    }

    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, URI uri, ByteBuf content) {
        this(httpVersion, method, uri.toString(), content);
    }

    private void init() {

        setUri(uriInfo.rawRelative());
        headers().set(HttpHeaders.Names.HOST, uriInfo.getHost());
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }
}