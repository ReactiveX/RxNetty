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
package io.reactivex.netty.protocol.http;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class CookiesHolderTest {

    @Test(timeout = 60000)
    public void testClientResponseHolder() throws Exception {
        DefaultHttpResponse headers = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        String cookie1Name = "PREF";
        String cookie1Value = "ID=a95756377b78e75e:FF=0:TM=1392709628:LM=1392709628:S=a5mOVvTB7DBkexgi";
        String cookie1Header = cookie1Name + '=' + cookie1Value
                               + "; expires=Thu, 18-Feb-2016 07:47:08 GMT;";
        headers.headers().add(SET_COOKIE, cookie1Header);

        CookiesHolder holder = CookiesHolder.newClientResponseHolder(headers.headers());
        Map<String,Set<Cookie>> cookies = holder.getAllCookies();

        assertThat("Cookies are null.", cookies, is(notNullValue()));
        assertThat("Cookies are empty.", cookies.values(), is(not(empty())));

        Set<Cookie> cookies1 = cookies.get(cookie1Name);

        assertThat("No cookies found with name: " + cookie1Name, cookies1, is(notNullValue()));
        assertThat("Unexpected number of cookies found.", cookies1, hasSize(1));

        Cookie cookieFound = cookies1.iterator().next();

        assertThat("Unexpected cookie name.", cookieFound.name(), equalTo(cookie1Name));
        assertThat("Unexpected cookie value.", cookieFound.value(), equalTo(cookie1Value));
    }

    @Test(timeout = 60000)
    public void testServerRequestHolder() throws Exception {
        DefaultHttpRequest headers = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "");
        String cookie1Name = "PREF";
        String cookie1Value = "ID=a95756377b78e75e:FF=0:TM=1392709628:LM=1392709628:S=a5mOVvTB7DBkexgi";
        Cookie cookie = new DefaultCookie(cookie1Name, cookie1Value);

        headers.headers().add(COOKIE, ClientCookieEncoder.STRICT.encode(cookie));

        CookiesHolder holder = CookiesHolder.newServerRequestHolder(headers.headers());
        Map<String, Set<Cookie>> cookies = holder.getAllCookies();

        assertThat("Cookies are null.", cookies, is(notNullValue()));
        assertThat("Cookies are empty.", cookies.values(), is(not(empty())));

        Set<Cookie> cookies1 = cookies.get(cookie1Name);
        assertThat("No cookies found with name: " + cookie1Name, cookies1, is(notNullValue()));
        assertThat("Unexpected number of cookies found.", cookies1, hasSize(1));

        Cookie cookieFound = cookies1.iterator().next();

        assertThat("Unexpected cookie name.", cookieFound.name(), equalTo(cookie1Name));
        assertThat("Unexpected cookie value.", cookieFound.value(), equalTo(cookie1Value));
    }
}