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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * A wrapper on a valid URI that provides additional information such as port number and relative path
 * This wrapper makes heavy assumption that the default values are for either HTTP or HTTPS.
 */
public class UriInfo {
    public static final Scheme DEFAULT_SCHEME = Scheme.HTTP;
    public static final int DEFAULT_PORT = 80;
    public static final int DEFAULT_SECURE_PORT = 443;

    private final URI uri;
    private final int port;
    private final Scheme scheme;
    private final String host;

    // We hide the constructor because we may introduce more builder methods later.
    private UriInfo(URI uri) {
        this.uri = uri;
        this.scheme = interpretScheme(uri.getScheme());
        this.port = interpretPort(uri.getPort());
        this.host = interpretHost(uri.getHost());
    }

    private int interpretPort(int port) {
        if (port > 0) {
            return port;

        }

        if (getScheme() == Scheme.HTTP) {
            return DEFAULT_PORT;
        }

        if (getScheme() == Scheme.HTTPS) {
            return DEFAULT_SECURE_PORT;
        }

        return -1;
    }

    private String interpretHost(String host) {
        if (host == null) {
            throw new IllegalArgumentException(String.format("Host can't be null for uri '%s' ", uri));
        }

        return host;
    }

    public URI getUri() {
        return uri;
    }

    public int getPort() {
        return port;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public String rawRelative() {
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getRawPath());

        String query = uri.getRawQuery();
        if (query != null && query.trim().length() > 0) {
            builder.append('?').append(query);
        }

        return builder.toString();
    }

    private Scheme interpretScheme(String scheme) {
        if (scheme == null) {
            return DEFAULT_SCHEME;
        }

        try {
            return Scheme.valueOf(scheme.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("The scheme '%s' is not supported yet. Supported schemes: %s", scheme, Arrays.toString(Scheme.values())));
        }
    }

    public static UriInfo fromUri(URI uri) {
        return new UriInfo(uri);
    }

    public static UriInfo fromUri(String uri) {
        try {
            URI uriObj = new URI(uri);
            if(uriObj.getHost() == null || uriObj.getHost().trim().isEmpty()) {
                throw new IllegalArgumentException(String.format("Given uri %s must have a host", uri));
            }
            return fromUri(uriObj);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("Given uri %s is not valid: %s", uri, e.getMessage()), e);
        }
    }

    public static enum Scheme {
        HTTP,
        HTTPS
    }

    public static void main(String[] args) throws Exception {
        URI uri = new URI("http://jenkins_slave-cf8d2895:50913/");
        System.out.println(UriInfo.fromUri(uri).getHost());

    }
}
