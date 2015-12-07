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

package io.reactivex.netty.protocol.http.internal;

import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import rx.observers.SafeSubscriber;

/**
 * A subscriber that can be reused if and only if not wrapped in a {@link SafeSubscriber}.
 */
final class UnsafeEmptySubscriber<T> extends Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(UnsafeEmptySubscriber.class);

    private final String msg;

    protected UnsafeEmptySubscriber(String msg) {
        this.msg = msg;
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        logger.error(msg, e);
    }

    @Override
    public void onNext(T o) {
        ReferenceCountUtil.release(o);
    }
}
