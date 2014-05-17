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

import io.reactivex.netty.client.RxClient;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.SerialSubscription;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An Rx {@link rx.Observable.Operator} which handles HTTP redirects. <br/>
 * The redirect behavior can be altered by supplying a custom implementation of {@link RedirectHandler}. By default this
 * uses {@link DefaultRedirectHandler}
 *
 * @author Nitesh Kant
 */
public class RedirectOperator<I, O>
        implements Observable.Operator<HttpClientResponse<O>, HttpClientResponse<O>> {

    public static final int DEFAULT_MAX_HOPS = 5;
    private final HttpClientRequest<I> originalRequest;
    private final RedirectHandler<I, O> redirectHandler;

    public RedirectOperator(HttpClientRequest<I> originalRequest, int maxHops, HttpClient<I, O> clientForRedirect) {
        this(originalRequest, new DefaultRedirectHandler<I, O>(maxHops, clientForRedirect));
    }

    public RedirectOperator(HttpClientRequest<I> originalRequest, int maxHops, HttpClient<I, O> clientForRedirect,
                            RxClient.ClientConfig config) {
        this(originalRequest, new DefaultRedirectHandler<I, O>(maxHops, clientForRedirect, config));
    }

    public RedirectOperator(HttpClientRequest<I> originalRequest, RedirectHandler<I, O> redirectHandler) {
        this.originalRequest = originalRequest;
        this.redirectHandler = redirectHandler;
    }

    @Override
    public Subscriber<? super HttpClientResponse<O>> call(final Subscriber<? super HttpClientResponse<O>> child) {

        final SerialSubscription serialSubscription = new SerialSubscription();
        // add serialSubscription so it gets unsubscribed if child is unsubscribed
        child.add(serialSubscription);

        final RedirectHandler.RedirectionContext redirectionContext =
                new RedirectHandler.RedirectionContext(originalRequest);

        Subscriber<HttpClientResponse<O>> toReturn = new RedirectSubscriber<O>(child, redirectionContext,
                                                                               serialSubscription, redirectHandler);

        serialSubscription.set(toReturn); // In the next redirect, this should get unsubcribed.
        return toReturn;
    }

    /**
     * A handler contract for handling HTTP redirects. This handler is used in the following way:
     * <ul>
     <li>After every response, {@link #requiresRedirect(RedirectionContext, HttpClientResponse)} is
     called to know whether the response requires a further redirection.</li>
     <li>If a response requires redirection, it checks whether the redirect limit has already been breached. This is
     asserted based on {@link #validate(RedirectionContext, HttpClientResponse)}.
     The reason for this not included in the previous call is that we want to differentiate between a response not
     requiring redirects vs a response requiring redirects but not being performed because of limits like max redirects
     allowed, redirect loops etc..</li>
     <li>If the redirect limit is not yet breached, then
     {@link #doRedirect(RedirectionContext, HttpClientRequest, HttpClientResponse)} will be called.</li>
     </ul>

     * @param <I> Content type of request sent over this handler.
     * @param <O> Content type of response received over this handler.
     */
    public interface RedirectHandler<I, O> {

        /**
         * Performs the redirect operation. This should at the least call {@link RedirectionContext#newLocation(String)}
         * with the new redirect location.
         *
         * @param context Redirection context.
         * @param originalRequest Original request that started this response processing.
         * @param redirectedResponse The response obtained from the HTTP call made prior to this redirect.
         *
         * @return The response after executing the redirect.
         */
        Observable<HttpClientResponse<O>> doRedirect(RedirectionContext context,
                                                     HttpClientRequest<I> originalRequest,
                                                     HttpClientResponse<O> redirectedResponse);

        /**
         * Asserts whether the passed {@code response} requires a redirect. If this returns {@code true} then
         * {@link RedirectHandler#doRedirect(RedirectionContext, HttpClientRequest, HttpClientResponse)}
         * will be called for this {@code response} if and only if the redirect is valid specified by
         * {@link RedirectHandler#validate(RedirectionContext, HttpClientResponse)}
         *
         * @param context Redirection context.
         * @param response The response to be evaluated for redirects.
         *
         * @return {@code true} if the response needs redirection, else {@code false}
         */
        boolean requiresRedirect(RedirectionContext context, HttpClientResponse<O> response);

        /**
         * This is invoked if a particular response requires a redirect as evaluated by
         * {@link RedirectHandler#requiresRedirect(RedirectionContext, HttpClientResponse)}. If this returns
         * {@code false} the redirect is not performed, instead an error is propagated. <p/>
         * This should throw an exception if the redirect is not valid. eg: If the
         * max redirects limit is 3 and the redirects till now are 2, then this method should thrown an exception.
         *
         *
         * @param context The redirection context.
         * @param redirectResponse The response to be evaluated for redirects.
         *
         * @throws HttpRedirectException if the redirect is not valid.
         */
        void validate(RedirectionContext context, HttpClientResponse<O> redirectResponse);

        class RedirectionContext {

            private final List<String> visitedLocations; // Is never updated concurrently as redirects are sequential.

            /*
             *Immutable list for the getter.
             */
            private List<String> visitedLocationsImmutable; // Is never updated concurrently as redirects are sequential.
            private volatile int redirectCount; // Can be shared across multiple event loops, so needs to be volatile.
            private URI nextRedirect;

            public RedirectionContext(@SuppressWarnings("rawtypes")HttpClientRequest originalRequest) {
                visitedLocations = new ArrayList<String>();
                String uri = originalRequest.getAbsoluteUri();
                visitedLocations.add(uri); // Original location must be added as visited to detect 1st level loop.
                visitedLocationsImmutable = Collections.unmodifiableList(visitedLocations);
                redirectCount = 0;
            }

            public void newLocation(String visitedLocation) {
                visitedLocations.add(visitedLocation);
                visitedLocationsImmutable = Collections.unmodifiableList(visitedLocations);
            }

            /**
             * Returns an immutable list of the visited locations in this request processing. If an update is required,
             * {@link #newLocation(String)} must be used.
             *
             * @return An immutable list of the visited locations in this request processing.
             */
            public List<String> getVisitedLocations() {
                return visitedLocationsImmutable;
            }

            /*Used only by the retry operator*/void onNewRedirect() {
                redirectCount++;
            }

            public int getRedirectCount() {
                return redirectCount;
            }

            public void setNextRedirect(URI nextRedirect) {
                this.nextRedirect = nextRedirect;
            }

            public URI getNextRedirect() {
                return nextRedirect;
            }
        }
    }

    private class RedirectSubscriber<O> extends Subscriber<HttpClientResponse<O>> {

        private final Subscriber<? super HttpClientResponse<O>> child;
        private final RedirectHandler.RedirectionContext redirectionContext;
        private final SerialSubscription serialSubscription;
        private final RedirectHandler<I, O> redirectHandler;
        private final AtomicBoolean finished = new AtomicBoolean();

        public RedirectSubscriber(Subscriber<? super HttpClientResponse<O>> child,
                                  RedirectHandler.RedirectionContext redirectionContext,
                                  SerialSubscription serialSubscription,
                                  RedirectHandler<I, O> redirectHandler) {
            this.child = child;
            this.redirectionContext = redirectionContext;
            this.serialSubscription = serialSubscription;
            this.redirectHandler = redirectHandler;
        }

        @Override
        public void onCompleted() {
            if (!isUnsubscribed() && finished.compareAndSet(false, true)) {
                child.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!isUnsubscribed() && finished.compareAndSet(false, true)) {
                child.onError(e);
            }
        }

        @Override
        public void onNext(HttpClientResponse<O> response) {
            if (isUnsubscribed() || finished.get()) {
                return;
            }
            if (redirectHandler.requiresRedirect(redirectionContext, response)) {
                try {
                    redirectHandler.validate(redirectionContext, response);
                    redirectionContext.onNewRedirect();
                    Observable<HttpClientResponse<O>> redirect = redirectHandler.doRedirect(redirectionContext,
                                                                                            originalRequest,
                                                                                            response);
                    RedirectSubscriber<O> newSub = copy();
                    serialSubscription.set(newSub); // Set is required first to avoid new subscribe before previous unsubscribe.
                    redirect.unsafeSubscribe(newSub);
                } catch (HttpRedirectException e) {
                    onError(e);
                }
            } else {
                child.onNext(response);
            }
        }

        public RedirectSubscriber<O> copy() {
            return new RedirectSubscriber<O>(child, redirectionContext, serialSubscription, redirectHandler);
        }
    }
}
