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
import io.reactivex.netty.client.RxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URI;
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
    private final RxClient.ClientConfig config;

    public DefaultRedirectHandler(int maxHops, HttpClient<I, O> client) {
        this.maxHops = maxHops;
        this.client = client;
        config = null;
    }

    public DefaultRedirectHandler(int maxHops, HttpClient<I, O> client, RxClient.ClientConfig config) {
        this.maxHops = maxHops;
        this.client = client;
        this.config = config;
    }

    @Override
    public Observable<HttpClientResponse<O>> doRedirect(RedirectionContext context,
                                                        HttpClientRequest<I> originalRequest,
                                                        HttpClientResponse<O> redirectedResponse) {
        URI nextRedirect = context.getNextRedirect(); // added by validate()

        if (logger.isDebugEnabled()) {
            logger.debug("Following redirect to location: " + nextRedirect + ". Redirect count: "
                         + context.getRedirectCount());
        }

        HttpClientRequest<I> redirectRequest = createRedirectRequest(originalRequest, nextRedirect,
                                                                     redirectedResponse.getStatus().code());

        return redirect(redirectRequest);
    }

    protected Observable<HttpClientResponse<O>> redirect(HttpClientRequest<I> redirectRequest) {
        return client.submit(redirectRequest, config);
    }

    @Override
    public boolean requiresRedirect(RedirectionContext context, HttpClientResponse<O> response) {
        int statusCode = response.getStatus().code();
        return Arrays.binarySearch(REDIRECTABLE_STATUS_CODES, statusCode) >= 0;
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

        validateUri(location, redirectUri);

        context.setNextRedirect(redirectUri);
    }

    protected String extractRedirectLocation(HttpClientResponse<O> redirectedResponse) {
        return redirectedResponse.getHeaders().get(HttpHeaders.Names.LOCATION);
    }

    protected void validateUri(String location, URI redirectUri) {
        if (!redirectUri.isAbsolute()) {
            // Redirect URI must be absolute
            throw new HttpRedirectException(InvalidRedirect,
                                            String.format("Redirect URI %s must be absolute", location));
        }

        String host = redirectUri.getHost();
        if (host == null) {
            throw new HttpRedirectException(InvalidRedirect,
                                            String.format("Location header %s does not contain a host name", location));
        }
    }

    protected <I> HttpClientRequest<I> createRedirectRequest(HttpClientRequest<I> original, URI redirectLocation,
                                                             int redirectStatus) {
        HttpRequest nettyRequest = original.getNettyRequest();
        nettyRequest.setUri(redirectLocation.toString() /*Uses the string with which this was created*/);

        if (redirectStatus == 303) {
            // according to HTTP spec, 303 mandates the change of request type to GET
            nettyRequest.setMethod(HttpMethod.GET);
        }

        HttpClientRequest<I> newRequest = new HttpClientRequest<I>(nettyRequest);

        if (redirectStatus != 303) {
            // if status code is 303, we can just leave the content factory to be null
            newRequest.contentFactory = original.contentFactory;
            newRequest.rawContentFactory = original.rawContentFactory;
        }
        return newRequest;
    }
}
