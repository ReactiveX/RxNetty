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

import rx.Observer;

/**
 * Delegates the {@link #onCompleted()} and {@link #onError(Throwable)} to all contained observers.
 *
 * @author Nitesh Kant
 */
public abstract class CompositeObserver<T> implements Observer<T> {

    @SuppressWarnings("rawtypes") private final Observer[] underlyingObservers;

    protected CompositeObserver(@SuppressWarnings("rawtypes") Observer... underlyingObservers) {
        this.underlyingObservers = underlyingObservers;
    }

    @Override
    public void onCompleted() {
        for (@SuppressWarnings("rawtypes") Observer underlyingObserver : underlyingObservers) {
            underlyingObserver.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        for (@SuppressWarnings("rawtypes") Observer underlyingObserver : underlyingObservers) {
            underlyingObserver.onError(e);
        }
    }
}
