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
 * @author Nitesh Kant
 */
public interface HttpContentHolder<T> {

    /**
     * Returns the content {@link Observable}. The content {@link Observable} can have one and only one subscription. <br>
     *
     * In case, multiple subscriptions are required, you must use {@link Observable#publish()}
     *
     * <h2>Subscriptions</h2>
     * It is mandatory to have atleast one subscription to the returned {@link Observable} or else it will increase
     * memory consumption as the underlying {@link Observable} buffers content untill subscription.
     *
     * <h2>Ignoring content</h2>
     * In case the consumer of this response, is not interested in the content, it should invoke {@link #ignoreContent()}
     * or else the content will remain in memory till the configured timeout.
     *
     * @return The content {@link Observable} which must have one and only one subscription.
     *
     * @see UnicastContentSubject
     */
    Observable<T> getContent();

    /**
     * This will ignore the content and any attempt to subscribe to the content {@link Observable} as returned by
     * {@link #getContent()} will result in invoking {@link rx.Observer#onError(Throwable)} on the subscriber.
     */
    void ignoreContent();
}
