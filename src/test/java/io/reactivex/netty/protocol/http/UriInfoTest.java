/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.protocol.http;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UriInfoTest {
    @Test
    public void testFullValidURIWillBeAccepted() {
        String host = "host";
        int port = 7777;
        String scheme = "https";
        String path = "path";

        String uri = String.format("%s://%s:%d/%s", scheme, host, port, path);

        UriInfo info = UriInfo.fromUri(uri);
        assertEquals(UriInfo.Scheme.valueOf(scheme.toUpperCase()), info.getScheme());
        assertEquals(host, info.getHost());
        assertEquals(port, info.getPort());
    }

    @Test
    public void testInvalidSchemesWillBeRejected() {
        try{
            String url = "invalidScheme://host:7000/abc";
            UriInfo.fromUri(url);
            fail(String.format("The uri %s should result in an exception because its scheme is not supported. ", url));
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testHttpPortHasDefaultValue() {
        String host = "host";
        String scheme = "http";
        String path = "path";

        String uri = String.format("%s://%s/%s", scheme, host, path);

        UriInfo info = UriInfo.fromUri(uri);
        assertEquals(UriInfo.Scheme.valueOf(scheme.toUpperCase()), info.getScheme());
        assertEquals(host, info.getHost());
        assertEquals(80, info.getPort());
    }

    @Test
    public void testHttpsPortHasDefaultValue() {
        String host = "host";
        String scheme = "https";
        String path = "path";

        String uri = String.format("%s://%s/%s", scheme, host, path);

        UriInfo info = UriInfo.fromUri(uri);
        assertEquals(UriInfo.Scheme.valueOf(scheme.toUpperCase()), info.getScheme());
        assertEquals(host, info.getHost());
        assertEquals(443, info.getPort());
    }

    @Test
    public void testSchemeHasDefaultValue() {
        String host = "host";
        int port = 7777;
        String path = "path";

        String uri = String.format("//%s:%d/%s", host, port, path);

        UriInfo info = UriInfo.fromUri(uri);
        assertEquals(UriInfo.Scheme.HTTP, info.getScheme());
        assertEquals(host, info.getHost());
        assertEquals(port, info.getPort());
    }

    @Test
    public void testHostIsRequired() throws Exception {
        String host = "host";
        int port = 7777;
        String path = "path";

        String uri = String.format("%s:%d/%s", host, port, path);

        try{
            UriInfo info = UriInfo.fromUri(uri);
            fail(String.format("The uri %s should be invalid and result a failure. ", uri));
        } catch(IllegalArgumentException e) {
            //expected
        }

        String edgeUri = "http://jenkins_slave-cf8d2895:50913/";

        try{
            UriInfo.fromUri(edgeUri);
            fail(String.format("The uri %s is valid but is interpreted as missing host. It should fail. ", edgeUri));
        } catch(IllegalArgumentException e) {
            // expected
        }

        try{
            UriInfo.fromUri(new URI(edgeUri));
            fail(String.format("The uri should result in missing host name, and therefore result in IllegalArgumentException ", edgeUri));
        } catch(IllegalArgumentException e){
            // expected
        }
    }

}
