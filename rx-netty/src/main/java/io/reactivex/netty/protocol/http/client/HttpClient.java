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

import java.util.concurrent.TimeUnit;

/**
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 */
public interface HttpClient<I, O> extends RxClient<HttpClientRequest<I>, HttpClientResponse<O>> {

    Observable<HttpClientResponse<O>> submit(HttpClientRequest<I> request);

    Observable<HttpClientResponse<O>> submit(HttpClientRequest<I> request, ClientConfig config);

    /**
     * A configuration to be used for this client.
     */
    class HttpClientConfig extends ClientConfig {

        public enum RedirectsHandling {Enable, Disable, Undefined}

        private String userAgent = "RxNetty Client";

        private RedirectsHandling followRedirect = RedirectsHandling.Undefined;
        private int maxRedirects = RedirectOperator.DEFAULT_MAX_HOPS;
        private long responseSubscriptionTimeoutMs = HttpClientResponse.DEFAULT_CONTENT_SUBSCRIPTION_TIMEOUT_MS;

        protected HttpClientConfig() {
            // Only the builder can create this instance, so that we can change the constructor signature at will.
        }

        protected HttpClientConfig(ClientConfig config) {
            super(config);
        }

        public String getUserAgent() {
            return userAgent;
        }

        public RedirectsHandling getFollowRedirect() {
            return followRedirect;
        }

        public int getMaxRedirects() {
            return maxRedirects;
        }

        public long getResponseSubscriptionTimeoutMs() {
            return responseSubscriptionTimeoutMs;
        }

        @Override
        public HttpClientConfig clone() throws CloneNotSupportedException {
            return (HttpClientConfig) super.clone();
        }

        public static class Builder extends AbstractBuilder<Builder, HttpClientConfig> {

            public Builder(HttpClientConfig defaultConfig) {
                super(null == defaultConfig ? new HttpClientConfig() : defaultConfig);
            }

            public Builder() {
                this(null);
            }

            public Builder userAgent(String userAgent) {
                config.userAgent = userAgent;
                return returnBuilder();
            }

            public Builder setFollowRedirect(boolean value) {
                config.followRedirect = value ? RedirectsHandling.Enable : RedirectsHandling.Disable;
                return returnBuilder();
            }

            public Builder followRedirect(int maxRedirects) {
                setFollowRedirect(true);
                config.maxRedirects = maxRedirects;
                return returnBuilder();
            }

            public Builder responseSubscriptionTimeout(long timeout, TimeUnit timeUnit) {
                config.responseSubscriptionTimeoutMs = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
                return returnBuilder();
            }

            public static HttpClientConfig newDefaultConfig() {
                return new Builder().build();
            }

            public static Builder fromDefaultConfig() {
                return from(newDefaultConfig());
            }

            public static Builder from(HttpClientConfig source) {
                try {
                    return new Builder(null == source ? newDefaultConfig() : source.clone());
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
