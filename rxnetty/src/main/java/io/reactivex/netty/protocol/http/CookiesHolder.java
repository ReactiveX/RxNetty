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
package io.reactivex.netty.protocol.http;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A holder of cookies parsed from the Http headers.
 *
 * @author Nitesh Kant
 */
public class CookiesHolder {

    private final HttpHeaders nettyHeaders;
    private final String cookiesHeaderName;
    private Map<String, Set<Cookie>> allCookies;
    private boolean cookiesParsed;

    private CookiesHolder(HttpHeaders nettyHeaders, String cookiesHeaderName) {
        this.nettyHeaders = nettyHeaders;
        this.cookiesHeaderName = cookiesHeaderName;
        allCookies = Collections.emptyMap();
    }

    public Map<String, Set<Cookie>> getAllCookies() {
        return _parseIfNeededAndGet();
    }

    public static CookiesHolder newClientResponseHolder(HttpHeaders headers) {
        return new CookiesHolder(headers, HttpHeaders.Names.SET_COOKIE);
    }

    public static CookiesHolder newServerRequestHolder(HttpHeaders headers) {
        return new CookiesHolder(headers, HttpHeaders.Names.COOKIE);
    }

    private synchronized Map<String, Set<Cookie>> _parseIfNeededAndGet() {
        if (cookiesParsed) { // This method is synchronized, so creates a memory barrier for this variable to be refreshed.
            return allCookies;
        }
        List<String> allCookieHeaders = nettyHeaders.getAll(cookiesHeaderName);
        Map<String, Set<Cookie>> cookies = new HashMap<String, Set<Cookie>>();
        for (String aCookieHeader : allCookieHeaders) {
            Set<Cookie> decode = CookieDecoder.decode(aCookieHeader);
            for (Cookie cookie : decode) {
                Set<Cookie> existingCookiesOfName = cookies.get(cookie.getName());
                if (null == existingCookiesOfName) {
                    existingCookiesOfName = new HashSet<Cookie>();
                    cookies.put(cookie.getName(), existingCookiesOfName);
                }
                existingCookiesOfName.add(cookie);
            }
        }
        allCookies = Collections.unmodifiableMap(cookies);
        cookiesParsed = true;
        return allCookies;
    }
}
