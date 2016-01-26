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

package io.reactivex.netty.client.internal;

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.HostConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

/**
 * A connection provider that only ever fetches a single host from the host stream provided to it.
 *
 * @param <W> The type of objects written on the connections created by this provider.
 * @param <R> The type of objects read from the connections created by this provider.
 */
public class SingleHostConnectionProvider<W, R> implements ConnectionProvider<W, R> {

    private static final Logger logger = LoggerFactory.getLogger(SingleHostConnectionProvider.class);

    private volatile ConnectionProvider<W, R> provider;

    public SingleHostConnectionProvider(Observable<HostConnector<W, R>> connectors) {
        connectors.toSingle()
                  .subscribe(new Action1<HostConnector<W, R>>() {
                      @Override
                      public void call(HostConnector<W, R> connector) {
                          provider = connector.getConnectionProvider();
                      }
                  }, new Action1<Throwable>() {
                      @Override
                      public void call(Throwable t) {
                          logger.error("Failed while fetching a host connector from a scalar host source", t);
                      }
                  });
    }

    @Override
    public Observable<Connection<R, W>> newConnectionRequest() {
        return null != provider ? provider.newConnectionRequest()
                : Observable.<Connection<R, W>>error(new IllegalStateException("No hosts available."));
    }
}
