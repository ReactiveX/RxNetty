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

package io.reactivex.netty.protocol.http;

import rx.Observable;

/**
 * An abstract implementation of {@link HttpContentHolder}
 *
 * @author Nitesh Kant
 */
public abstract class AbstractHttpContentHolder<T> implements HttpContentHolder<T> {

    protected final UnicastContentSubject<T> content;

    protected AbstractHttpContentHolder(UnicastContentSubject<T> content) {
        this.content = content;
    }

    @Override
    public Observable<T> getContent() {
        return content;
    }

    @Override
    public void ignoreContent() {
        content.disposeIfNotSubscribed();
    }
}
