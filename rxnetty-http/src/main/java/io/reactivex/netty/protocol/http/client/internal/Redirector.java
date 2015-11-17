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

package io.reactivex.netty.protocol.http.client.internal;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.HttpRedirectException;
import io.reactivex.netty.protocol.http.internal.VoidToAnythingCast;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.protocol.http.client.HttpRedirectException.Reason.*;

public class Redirector<I, O> implements Func1<HttpClientResponse<O>, Observable<HttpClientResponse<O>>> {

    public static final int DEFAULT_MAX_REDIRECTS = 5;

    private static final Logger logger = LoggerFactory.getLogger(Redirector.class);

    private static final int[] REDIRECTABLE_STATUS_CODES = {301, 302, 303, 307, 308};

    static {
        Arrays.sort(REDIRECTABLE_STATUS_CODES); // Required as we do binary search. This is a safety net in case the
                                                // array is modified (code change) & is not sorted.
    }

    private final List<String> visitedLocations; // Is never updated concurrently as redirects are sequential.
    private final int maxHops;
    private final AtomicInteger redirectCount; // Can be shared across multiple event loops, so needs to be thread-safe.
    private volatile HttpResponseStatus lastRedirectStatus;
    private final TcpClient<?, HttpClientResponse<O>> client;

    private RawRequest<I, O> originalRequest;

    public Redirector(int maxHops, TcpClient<?, HttpClientResponse<O>> client) {
        this.maxHops = maxHops;
        this.client = client;
        visitedLocations = new ArrayList<>();
        redirectCount = new AtomicInteger();
    }

    public Redirector(TcpClient<?, HttpClientResponse<O>> client) {
        this(DEFAULT_MAX_REDIRECTS, client);
    }

    public void setOriginalRequest(RawRequest<I, O> originalRequest) {
        if (null != this.originalRequest) {
            throw new IllegalStateException("Original request is already set.");
        }
        this.originalRequest = originalRequest;
        visitedLocations.add(originalRequest.getHeaders().uri());
    }

    @Override
    public Observable<HttpClientResponse<O>> call(HttpClientResponse<O> response) {

        Observable<HttpClientResponse<O>> toReturn;

        if (null == originalRequest) {
            toReturn = Observable.error(new IllegalStateException("Raw request not available to the redirector."));
        } else if (requiresRedirect(response)) {
            String location = extractRedirectLocation(response);

            if (location == null) {
                toReturn = Observable.error(new HttpRedirectException(InvalidRedirect, "No redirect location found."));
            } else if (visitedLocations.contains(location)) {
                // this forms a loop
                toReturn = Observable.error(new HttpRedirectException(RedirectLoop,
                                                                      "Redirection contains a loop. Last requested location: "
                                                                      + location));
            } else if (redirectCount.get() >= maxHops) {
                toReturn = Observable.error(new HttpRedirectException(TooManyRedirects,
                                                                      "Too many redirects. Max redirects: " + maxHops));
            } else {
                URI redirectUri;

                try {
                    redirectUri = new URI(location);

                    lastRedirectStatus = response.getStatus();

                    redirectCount.incrementAndGet();

                    toReturn = createRedirectRequest(originalRequest, redirectUri, lastRedirectStatus.code());
                } catch (Exception e) {
                    toReturn = Observable.error(new HttpRedirectException(InvalidRedirect,
                                                                          "Location is not a valid URI. Provided location: "
                                                                          + location, e));
                }
            }

        } else {
            return Observable.just(response);
        }

        return response.discardContent()
                       .map(new VoidToAnythingCast<HttpClientResponse<O>>())
                       .ignoreElements()
                       .concatWith(toReturn);


    }

    public boolean requiresRedirect(HttpClientResponse<O> response) {
        int statusCode = response.getStatus().code();
        boolean requiresRedirect = false;
        // This class only supports relative redirects as an HttpClient is always tied to a host:port combo and hence
        // can not do an absolute redirect.
        if (Arrays.binarySearch(REDIRECTABLE_STATUS_CODES, statusCode) >= 0) {
            String location = extractRedirectLocation(response);
            // Only process relative URIs: Issue https://github.com/ReactiveX/RxNetty/issues/270
            requiresRedirect = null == location || !location.startsWith("http");
        }

        if (requiresRedirect && statusCode != HttpResponseStatus.SEE_OTHER.code()) {
            HttpMethod originalMethod = originalRequest.getHeaders().method();
            // If the Method is not HEAD/GET do not auto redirect
            requiresRedirect = originalMethod == HttpMethod.GET || originalMethod == HttpMethod.HEAD;
        }

        return requiresRedirect;
    }

    protected String extractRedirectLocation(HttpClientResponse<O> redirectedResponse) {
        return redirectedResponse.getHeader(HttpHeaderNames.LOCATION);
    }

    protected HttpClientRequest<I, O> createRedirectRequest(RawRequest<I, O> original, URI redirectLocation,
                                                            int redirectStatus) {

        String redirectUri = getNettyRequestUri(redirectLocation, original.getHeaders().uri(), redirectStatus);

        RawRequest<I, O> redirectRequest = original.setUri(redirectUri);

        if (redirectStatus == 303) {
            // according to HTTP spec, 303 mandates the change of request type to GET
            // If it is a get, then the content is not to be sent.
            redirectRequest = RawRequest.create(redirectRequest.getHeaders().protocolVersion(), HttpMethod.GET,
                                                redirectUri, this, original.getCorrelator());
        }

        return HttpClientRequestImpl.create(redirectRequest, client);
    }

    protected static String getNettyRequestUri(URI uri, String originalUriString, int redirectStatus) {
        StringBuilder sb = new StringBuilder();
        if (uri.getRawPath() != null) {
            sb.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            sb.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            sb.append('#').append(uri.getRawFragment());
        } else if(redirectStatus >= 300) {
            // http://tools.ietf.org/html/rfc7231#section-7.1.2 suggests that the URI fragment should be carried over to
            // the redirect location if not exists in the redirect location.
            // Issue: https://github.com/ReactiveX/RxNetty/issues/271
            try {
                URI originalUri = new URI(originalUriString);
                if (originalUri.getRawFragment() != null) {
                    sb.append('#').append(originalUri.getRawFragment());
                }
            } catch (URISyntaxException e) {
                logger.warn("Error parsing original request URI during redirect. " +
                            "This means that the path fragment if any in the original request will not be inherited " +
                            "by the redirect.", e);
            }
        }
        return sb.toString();
    }

}
