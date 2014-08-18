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

package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 * @author Nitesh Kant
 */
public class CookieTest {

    @Test
    public void testGetCookie() throws Exception {
        DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        String cookie1Name = "PREF";
        String cookie1Value = "ID=a95756377b78e75e:FF=0:TM=1392709628:LM=1392709628:S=a5mOVvTB7DBkexgi";
        String cookie1Domain = ".google.com";
        String cookie1Path = "/";
        String cookie1Header = cookie1Name + '=' + cookie1Value
                               + "; expires=Thu, 18-Feb-2016 07:47:08 GMT; path=" + cookie1Path + "; domain=" + cookie1Domain;
        nettyResponse.headers().add(HttpHeaders.Names.SET_COOKIE, cookie1Header);
        HttpClientResponse<ByteBuf> response = new HttpClientResponse<ByteBuf>(nettyResponse, UnicastContentSubject.<ByteBuf>createWithoutNoSubscriptionTimeout());
        Map<String,Set<Cookie>> cookies = response.getCookies();
        Assert.assertNotNull("Cookies are null.", cookies);
        Assert.assertEquals("Cookies are empty.", 1, cookies.size());
        Set<Cookie> cookies1 = cookies.get(cookie1Name);
        Assert.assertNotNull("No cookies found with name: " + cookie1Name, cookies1);
        Assert.assertEquals("Unexpected number of cookies found.", 1, cookies1.size());
        Cookie cookieFound = cookies1.iterator().next();
        Assert.assertEquals("unexpected cookie name.", cookie1Name, cookieFound.getName());
        Assert.assertEquals("unexpected cookie value.", cookie1Value, cookieFound.getValue());
        Assert.assertEquals("unexpected cookie path.", cookie1Path, cookieFound.getPath());
        Assert.assertEquals("unexpected cookie domain.", cookie1Domain, cookieFound.getDomain());
    }

    @Test
    public void testSetCookie() throws Exception {
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "");
        String cookie1Name = "PREF";
        String cookie1Value = "ID=a95756377b78e75e:FF=0:TM=1392709628:LM=1392709628:S=a5mOVvTB7DBkexgi";
        String cookie1Domain = ".google.com";
        String cookie1Path = "/";
        Cookie cookie = new DefaultCookie(cookie1Name, cookie1Value);
        cookie.setPath(cookie1Path);
        cookie.setDomain(cookie1Domain);
        new HttpClientRequest<ByteBuf>(nettyRequest).withCookie(cookie);
        String cookieHeader = nettyRequest.headers().get(HttpHeaders.Names.COOKIE);
        Assert.assertNotNull("No cookie header found.", cookieHeader);
        Set<Cookie> decodeCookies = CookieDecoder.decode(cookieHeader);
        Assert.assertNotNull("No cookie found with name.", decodeCookies);
        Assert.assertEquals("Unexpected number of cookies.", 1, decodeCookies.size());
        Cookie decodedCookie = decodeCookies.iterator().next();
        Assert.assertEquals("Unexpected cookie name.", cookie1Name, decodedCookie.getName());
        Assert.assertEquals("Unexpected cookie path.", cookie1Path, decodedCookie.getPath());
        Assert.assertEquals("Unexpected cookie domain.", cookie1Domain, decodedCookie.getDomain());
    }
}
