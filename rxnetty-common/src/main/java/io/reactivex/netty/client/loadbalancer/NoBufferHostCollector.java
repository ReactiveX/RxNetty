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

import rx.Single;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link HostCollector} implementation that does not buffer any updates and hence emits a new list for every new
 * host received or a host removed.
 */
public class NoBufferHostCollector implements HostCollector {

    private final boolean allowDuplicates;

    public NoBufferHostCollector() {
        this(false);
    }

    public NoBufferHostCollector(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public <W, R> Func1<HostUpdate<W, R>, Single<List<HostHolder<W, R>>>> newCollector() {
        return new Func1<HostUpdate<W, R>, Single<List<HostHolder<W, R>>>>() {

            private volatile List<HostHolder<W, R>> currentList = Collections.emptyList();

            @Override
            public Single<List<HostHolder<W, R>>> call(HostUpdate<W, R> update) {
                List<HostHolder<W, R>> newList = null;

                switch (update.getAction()) {
                case Add:
                    if (allowDuplicates || !currentList.contains(update.getHostHolder())) {
                        newList = new ArrayList<>(currentList);
                        newList.add(update.getHostHolder());
                    }
                    break;
                case Remove:
                    newList = new ArrayList<>(currentList);
                    newList.remove(update.getHostHolder());
                    break;
                }

                if (null != newList) {
                    currentList = newList;
                }

                return Single.just(currentList);
            }
        };
    }
}
