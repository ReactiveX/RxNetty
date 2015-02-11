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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.NoOpChannelHandlerContext;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class HttpServerRequestUriTest {

    @Test
    public void testRequestUri() throws Exception {
        String path = "a/b/c";
        String qp1Name = "qp1";
        String qp1Val = "qp1Val";
        String qp2Name = "qp2";
        String qp2Val = "qp2Val";
        String qp2Val2 = "qp2Val222";
        String queryString = qp1Name + '=' + qp1Val + '&' + qp2Name + '=' + qp2Val + '&' + qp2Name + '=' + qp2Val2 ;
        String uri = path + '?' + queryString;
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpServerRequest<ByteBuf> request = newServerRequest(nettyRequest);
        Assert.assertEquals("Unexpected uri string", uri, request.getUri());
        Assert.assertEquals("Unexpected query string", queryString,request.getQueryString());
        Assert.assertEquals("Unexpected path string", path, request.getPath());
        Map<String,List<String>> qpsGot = request.getQueryParameters();
        Assert.assertNotNull("Got null query parameters", qpsGot);
        Assert.assertEquals("Unexpected number of query parameters", 2, qpsGot.size());
        List<String> qp1Got = qpsGot.get(qp1Name);
        Assert.assertNotNull("Got no query parameters with name: " + qp1Name, qp1Got);
        Assert.assertEquals("Unexpected number of query parameters with name: " + qp1Name, 1, qp1Got.size());
        Assert.assertEquals("Unexpected query parameter value with name: " + qp1Name, qp1Val, qp1Got.get(0));

        List<String> qp2Got = qpsGot.get(qp2Name);
        Assert.assertNotNull("Got no query parameters with name: " + qp2Name, qp2Got);
        Assert.assertEquals("Unexpected number of query parameters with name: " + qp2Name, 2, qp2Got.size());
        Assert.assertEquals("Unexpected query parameter value with name: " + qp2Name, qp2Got.get(0), qp2Val);
        Assert.assertEquals("Unexpected query parameter second value with name: " + qp2Name, qp2Got.get(1), qp2Val2);
    }

    @Test
    public void testEmptyQueryString() throws Exception {
        String path = "a/b/c";
        String uri = path + '?';
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpServerRequest<ByteBuf> request = newServerRequest(nettyRequest);
        Assert.assertEquals("Unexpected uri string", uri, request.getUri());
        Assert.assertEquals("Unexpected query string", "", request.getQueryString());
    }

    @Test
    public void testAbsentQueryString() throws Exception {
        String path = "a/b/c";
        String uri = path;
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpServerRequest<ByteBuf> request = newServerRequest(nettyRequest);
        Assert.assertEquals("Unexpected uri string", uri, request.getUri());
        Assert.assertEquals("Unexpected query string", "", request.getQueryString());
    }

    @Test
    public void testAbsolute() throws Exception {
        String uri = "http://localhost:54321/a/b|c?foo=bar|baz";
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpServerRequest<ByteBuf> request = newServerRequest(nettyRequest);
        Assert.assertEquals("Unexpected uri string", uri, request.getUri());
        Assert.assertEquals("Unexpected query string", "foo=bar|baz", request.getQueryString());
        Assert.assertEquals("Unexpected path string", "/a/b|c", request.getPath());
    }

    protected HttpServerRequest<ByteBuf> newServerRequest(DefaultHttpRequest nettyRequest) {
        Channel noOpChannel = new NoOpChannelHandlerContext().channel();
        return new HttpServerRequest<ByteBuf>(noOpChannel, nettyRequest,
                                              UnicastContentSubject.<ByteBuf>createWithoutNoSubscriptionTimeout());
    }
}
