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

import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsPublisher;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * @param <I> The request object type for this client.
 * @param <O> The response object type for this client.
 *
 * @author Nitesh Kant
 */
public interface RxClient<I, O> extends MetricEventsPublisher<ClientMetricsEvent<?>> {

    /**
     * Creates exactly one new connection for every subscription to the returned observable.
     *
     * @return A new obserbvable which creates a single connection for every connection.
     */
    Observable<ObservableConnection<O, I>> connect();

    /**
     * Shutdown this client.
     */
    void shutdown();

    /**
     * A unique name for this client.
     *
     * @return A unique name for this client.
     */
    String name();

    /**
     * A configuration to be used for this client.
     */
    class ClientConfig implements Cloneable {

        public static final long NO_TIMEOUT = -1;

        private long readTimeoutInMillis = NO_TIMEOUT;

        protected ClientConfig() {
            // Only the builder can create this instance, so that we can change the constructor signature at will.
        }

        protected ClientConfig(ClientConfig config) {
            readTimeoutInMillis = config.readTimeoutInMillis;
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

        @Override
        public ClientConfig clone() throws CloneNotSupportedException {
            return (ClientConfig) super.clone();
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

            public Builder() {
                this(null);
            }

            public static ClientConfig newDefaultConfig() {
                return new Builder().build();
            }
        }
    }

    class ServerInfo {

        public static final String DEFAULT_HOST = "localhost";
        public static final int DEFAULT_PORT = 80;
        private final String host;
        private final int port;

        public ServerInfo() {
            this(DEFAULT_HOST, DEFAULT_PORT);
        }

        public ServerInfo(String host) {
            this(host, DEFAULT_PORT);
        }

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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (host == null ? 0 : host.hashCode());
            result = prime * result + port;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ServerInfo other = (ServerInfo) obj;
            if (host == null) {
                if (other.host != null) {
                    return false;
                }
            } else if (!host.equals(other.host)) {
                return false;
            }
            if (port != other.port) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "ServerInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
        }
    }
}
