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

import rx.Observable;

import java.net.SocketAddress;

public final class Host {

    private final SocketAddress host;
    private final Observable<Void> closeNotifier;

    public Host(SocketAddress host) {
        this(host, Observable.<Void>never());
    }

    public Host(SocketAddress host, Observable<Void> closeNotifier) {
        this.host = host;
        this.closeNotifier = closeNotifier;
    }

    public SocketAddress getHost() {
        return host;
    }

    public Observable<Void> getCloseNotifier() {
        return closeNotifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Host)) {
            return false;
        }

        Host host1 = (Host) o;

        if (host != null? !host.equals(host1.host) : host1.host != null) {
            return false;
        }
        return closeNotifier != null? closeNotifier.equals(host1.closeNotifier) : host1.closeNotifier == null;

    }

    @Override
    public int hashCode() {
        int result = host != null? host.hashCode() : 0;
        result = 31 * result + (closeNotifier != null? closeNotifier.hashCode() : 0);
        return result;
    }
}
