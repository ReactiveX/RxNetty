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

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static io.reactivex.netty.protocol.http.client.HttpRedirectException.Reason.InvalidRedirect;
import static io.reactivex.netty.protocol.http.client.HttpRedirectException.Reason.RedirectLoop;
import static io.reactivex.netty.protocol.http.client.HttpRedirectException.Reason.TooManyRedirects;

/**
* @author Nitesh Kant
*/
public class DefaultRedirectHandler<I, O> implements RedirectOperator.RedirectHandler<I, O> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRedirectHandler.class);

    private static final int[] REDIRECTABLE_STATUS_CODES = {301, 302, 303, 307, 308};

    static {
        Arrays.sort(REDIRECTABLE_STATUS_CODES); // Required as we do binary search. This is a safety net in case the
                                                // array is modified (code change) & is not sorted.
    }

    private final int maxHops;
    private final HttpClient<I, O> client;

    public DefaultRedirectHandler(int maxHops, HttpClient<I, O> client) {
        this.maxHops = maxHops;
        this.client = client;
    }

    @Override
    public Observable<HttpClientResponse<O>> doRedirect(RedirectionContext context,
                                                        HttpClientRequest<I> originalRequest,
                                                        HttpClient.HttpClientConfig config) {
        URI nextRedirect = context.getNextRedirect(); // added by validate()

        if (logger.isDebugEnabled()) {
            logger.debug("Following redirect to location: " + nextRedirect + ". Redirect count: "
                         + context.getRedirectCount());
        }

        HttpClientRequest<I> redirectRequest = createRedirectRequest(originalRequest, nextRedirect,
                                                                     context.getLastRedirectStatus().code());

        return redirect(redirectRequest, config);
    }

    protected Observable<HttpClientResponse<O>> redirect(HttpClientRequest<I> redirectRequest,
                                                         HttpClient.HttpClientConfig config) {
        return client.submit(redirectRequest, config);
    }

    @Override
    public boolean requiresRedirect(RedirectionContext context, HttpClientResponse<O> response) {
        int statusCode = response.getStatus().code();
        // This class only supports relative redirects as an HttpClient is always tied to a host:port combo and hence
        // can not do an absolute redirect.
        if (Arrays.binarySearch(REDIRECTABLE_STATUS_CODES, statusCode) >= 0) {
            String location = extractRedirectLocation(response);
            return !location.startsWith("http"); // Only process relative URIs: Issue https://github.com/ReactiveX/RxNetty/issues/270
        }
        return false;
    }

    @Override
    public void validate(RedirectionContext context, HttpClientResponse<O> redirectResponse) {
        String location = extractRedirectLocation(redirectResponse);

        if (location == null) {
            throw new HttpRedirectException(InvalidRedirect, "No redirect location found.");
        }

        if (context.getVisitedLocations().contains(location)) {
            // this forms a loop
            throw new HttpRedirectException(RedirectLoop, "Redirection contains a loop. Last requested location: "
                                                          + location);
        }

        if (context.getRedirectCount() >= maxHops) {
            throw new HttpRedirectException(TooManyRedirects, "Too many redirects. Max redirects: " + maxHops);
        }

        URI redirectUri;

        try {
            redirectUri = new URI(location);
        } catch (Exception e) {
            throw new HttpRedirectException(InvalidRedirect, "Location is not a valid URI. Provided location: "
                                                             + location, e);
        }

        context.setNextRedirect(redirectUri);
    }

    protected String extractRedirectLocation(HttpClientResponse<O> redirectedResponse) {
        return redirectedResponse.getHeaders().get(HttpHeaders.Names.LOCATION);
    }

    protected HttpClientRequest<I> createRedirectRequest(HttpClientRequest<I> original, URI redirectLocation,
                                                         int redirectStatus) {
        HttpRequest nettyRequest = original.getNettyRequest();
        nettyRequest.setUri(getNettyRequestUri(redirectLocation, original.getUri(), redirectStatus));

        HttpClientRequest<I> newRequest = new HttpClientRequest<I>(nettyRequest, original);

        if (redirectStatus == 303) {
            // according to HTTP spec, 303 mandates the change of request type to GET
            nettyRequest.setMethod(HttpMethod.GET);
            // If it is a get, then the content is not to be sent.
            newRequest.removeContent();
        }
        return newRequest;
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
