/*
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex.netty.client;

import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import rx.Subscription;

public class HostConnector<W, R> implements EventSource<ClientEventListener> {

    private final Host host;
    private final ConnectionProvider<W, R> connectionProvider;
    @SuppressWarnings("rawtypes")
    private final EventSource eventSource;
    private final EventPublisher publisher;
    private final ClientEventListener clientPublisher;

    public HostConnector(Host host, ConnectionProvider<W, R> connectionProvider,
                         EventSource<? extends ClientEventListener> eventSource, EventPublisher publisher,
                         ClientEventListener clientPublisher) {
        this.host = host;
        this.connectionProvider = connectionProvider;
        this.eventSource = eventSource;
        this.publisher = publisher;
        this.clientPublisher = clientPublisher;
    }

    public HostConnector(HostConnector<W, R> source, ConnectionProvider<W, R> connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.host = source.host;
        this.eventSource = source.eventSource;
        this.clientPublisher = source.clientPublisher;
        this.publisher = source.publisher;
    }

    public Host getHost() {
        return host;
    }

    public ConnectionProvider<W, R> getConnectionProvider() {
        return connectionProvider;
    }

    public ClientEventListener getClientPublisher() {
        return clientPublisher;
    }

    public EventPublisher getEventPublisher() {
        return publisher;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Subscription subscribe(ClientEventListener listener) {
        return eventSource.subscribe(listener);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HostConnector)) {
            return false;
        }

        HostConnector<?, ?> that = (HostConnector<?, ?>) o;

        if (host != null? !host.equals(that.host) : that.host != null) {
            return false;
        }
        if (connectionProvider != null? !connectionProvider.equals(that.connectionProvider) :
                that.connectionProvider != null) {
            return false;
        }
        if (eventSource != null? !eventSource.equals(that.eventSource) : that.eventSource != null) {
            return false;
        }
        if (publisher != null? !publisher.equals(that.publisher) : that.publisher != null) {
            return false;
        }
        return clientPublisher != null? clientPublisher.equals(that.clientPublisher) : that.clientPublisher == null;

    }

    @Override
    public int hashCode() {
        int result = host != null? host.hashCode() : 0;
        result = 31 * result + (connectionProvider != null? connectionProvider.hashCode() : 0);
        result = 31 * result + (eventSource != null? eventSource.hashCode() : 0);
        result = 31 * result + (publisher != null? publisher.hashCode() : 0);
        result = 31 * result + (clientPublisher != null? clientPublisher.hashCode() : 0);
        return result;
    }
}
