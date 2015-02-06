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

package io.reactivex.netty.protocol.http.server;

import org.junit.Assert;
import org.junit.Test;

public class UriInfoHolderTest {

    @Test
    public void relativeUri() throws Exception {
        UriInfoHolder holder = new UriInfoHolder("/foo/%20bar");
        Assert.assertEquals("/foo/%20bar", holder.getRawUriString());
        Assert.assertEquals("/foo/%20bar", holder.getPath());
        Assert.assertEquals("", holder.getQueryString());
    }

    @Test
    public void relativeUriWithQuery() throws Exception {
        UriInfoHolder holder = new UriInfoHolder("/foo/%20bar?foo=bar%3D");
        Assert.assertEquals("/foo/%20bar?foo=bar%3D", holder.getRawUriString());
        Assert.assertEquals("/foo/%20bar", holder.getPath());
        Assert.assertEquals("foo=bar%3D", holder.getQueryString());
    }

    // This test will fail with a strict URI parser
    @Test
    public void relativeUriWithQueryUnescaped() throws Exception {
        UriInfoHolder holder = new UriInfoHolder("/foo/%20bar?foo=bar|||");
        Assert.assertEquals("/foo/%20bar?foo=bar|||", holder.getRawUriString());
        Assert.assertEquals("/foo/%20bar", holder.getPath());
        Assert.assertEquals("foo=bar|||", holder.getQueryString());
    }

    @Test
    public void relativeUriNoStartingSlash() throws Exception {
        UriInfoHolder holder = new UriInfoHolder("foo/bar");
        Assert.assertEquals("foo/bar", holder.getRawUriString());
        Assert.assertEquals("foo/bar", holder.getPath());
        Assert.assertEquals("", holder.getQueryString());
    }

    @Test
    public void absoluteUri() throws Exception {
        UriInfoHolder holder = new UriInfoHolder("http://www.foo.com/foo/%20bar");
        Assert.assertEquals("http://www.foo.com/foo/%20bar", holder.getRawUriString());
        Assert.assertEquals("/foo/%20bar", holder.getPath());
        Assert.assertEquals("", holder.getQueryString());
    }

    @Test
    public void absoluteUriWithQuery() throws Exception {
        UriInfoHolder holder = new UriInfoHolder("http://www.foo.com/foo/%20bar?foo=bar%3D");
        Assert.assertEquals("http://www.foo.com/foo/%20bar?foo=bar%3D", holder.getRawUriString());
        Assert.assertEquals("/foo/%20bar", holder.getPath());
        Assert.assertEquals("foo=bar%3D", holder.getQueryString());
    }

}
