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
package io.reactivex.netty.client;

import io.reactivex.netty.ObservableConnection;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public interface RxClient<I, O> {

    /**
     * Creates exactly one new connection for every subscription to the returned observable.
     *
     * @return A new obserbvable which creates a single connection for every connection.
     */
    Observable<ObservableConnection<O, I>> connect();

    /**
     * A configuration to be used for this client.
     */
    class ClientConfig {

        public static final ClientConfig DEFAULT_CONFIG = new ClientConfig();

        public static final long NO_TIMEOUT = -1;

        private long readTimeoutInMillis = NO_TIMEOUT;

        protected ClientConfig() {
            // Only the builder can create this instance, so that we can change the constructor signature at will.
        }

        /**
         * Returns the set read timeout in milliseconds.
         *
         * @return The read timeout in milliseconds or {@link #NO_TIMEOUT} if there isn't any timeout set.
         */
        public long getReadTimeoutInMillis() {
            return readTimeoutInMillis;
        }

        void setReadTimeoutInMillis(long readTimeoutInMillis) {
            this.readTimeoutInMillis = readTimeoutInMillis;
        }

        public boolean isReadTimeoutSet() {
            return NO_TIMEOUT != readTimeoutInMillis;
        }

        @SuppressWarnings("rawtypes")
        protected static abstract class AbstractBuilder<B extends AbstractBuilder, C extends ClientConfig> {

            protected final C config;

            protected AbstractBuilder(C config) {
                this.config = config;
            }

            public B readTimeout(int timeout, TimeUnit timeUnit) {
                config.setReadTimeoutInMillis(TimeUnit.MILLISECONDS.convert(timeout, timeUnit));
                return returnBuilder();
            }

            @SuppressWarnings("unchecked")
            protected B returnBuilder() {
                return (B) this;
            }

            public C build() {
                return config;
            }
        }

        public static class Builder extends AbstractBuilder<Builder, ClientConfig> {

            public Builder(ClientConfig defaultConfig) {
                super(null == defaultConfig ? new ClientConfig() : defaultConfig);
            }
        }
    }

    class ServerInfo {

        private final String host;
        private final int port;

        public ServerInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
