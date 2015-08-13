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

package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class Http10Test {

    @Rule
    public final HttpServerRule rule = new HttpServerRule();

    @Test(timeout = 60000)
    public void testHttp1_0Response() throws Exception {
        rule.setServer(rule.getServer().sendHttp10ResponseFor10Request(true));
        rule.startServer();

        final HttpClientRequest<ByteBuf, ByteBuf> request =
                rule.getClient().createRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");

        final HttpClientResponse<ByteBuf> response = rule.sendRequest(request);

        assertThat("Unexpected HTTP version.", response.getHttpVersion(), is(HttpVersion.HTTP_1_0));
        assertThat("Unexpected keep-alive value.", response.isKeepAlive(), is(false));
        assertThat("Unexpected transfer encoding.", response.isTransferEncodingChunked(), is(false));

        rule.assertResponseContent(response);
    }

    @Test(timeout = 60000)
    public void testHttp1_1Response() throws Exception {
        rule.getServer().sendHttp10ResponseFor10Request(true);
        rule.startServer();

        final HttpClientRequest<ByteBuf, ByteBuf> request =
                rule.getClient().createRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");

        final HttpClientResponse<ByteBuf> response = rule.sendRequest(request);

        assertThat("Unexpected HTTP version.", response.getHttpVersion(), is(HttpVersion.HTTP_1_1));
        assertThat("Unexpected keep-alive value.", response.isKeepAlive(), is(false));
        assertThat("Unexpected transfer encoding.", response.isTransferEncodingChunked(), is(false));

        rule.assertResponseContent(response);
    }

}
