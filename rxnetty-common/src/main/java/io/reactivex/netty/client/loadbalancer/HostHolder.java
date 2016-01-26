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

package io.reactivex.netty.client.loadbalancer;

import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.events.ClientEventListener;

public class HostHolder<W, R> {

    private final HostConnector<W, R> connector;
    private final ClientEventListener eventListener;

    public HostHolder(HostConnector<W, R> connector, ClientEventListener eventListener) {
        this.connector = connector;
        this.eventListener = eventListener;
    }

    public HostConnector<W, R> getConnector() {
        return connector;
    }

    public ClientEventListener getEventListener() {
        return eventListener;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HostHolder)) {
            return false;
        }

        HostHolder<?, ?> that = (HostHolder<?, ?>) o;

        if (connector != null? !connector.equals(that.connector) : that.connector != null) {
            return false;
        }
        return eventListener != null? eventListener.equals(that.eventListener) : that.eventListener == null;

    }

    @Override
    public int hashCode() {
        int result = connector != null? connector.hashCode() : 0;
        result = 31 * result + (eventListener != null? eventListener.hashCode() : 0);
        return result;
    }
}
