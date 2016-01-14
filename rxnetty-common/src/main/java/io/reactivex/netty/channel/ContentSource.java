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

package io.reactivex.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * A source for any content/data read from a channel.
 *
 * <h2>Managing {@link ByteBuf} lifecycle.</h2>
 *
 * If this source emits {@link ByteBuf} or a {@link ByteBufHolder}, using {@link #autoRelease()} will release the buffer
 * after emitting it from this source.
 *
 * <h2>Replaying content</h2>
 *
 * Since, the content read from a channel is not re-readable, this also provides a {@link #replayable()} function that
 * produces a source which can be subscribed multiple times to replay the same data. This is specially useful if the
 * content read from one channel is written on to another with an option to retry.
 *
 * @param <T>
 */
public final class ContentSource<T> extends Observable<T> {

    private ContentSource(final Observable<T> source) {
        super(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                source.unsafeSubscribe(subscriber);
            }
        });
    }

    public ContentSource(final Channel channel, final Func1<Subscriber<? super T>, Object> subscriptionEventFactory) {
        super(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                channel.pipeline()
                       .fireUserEventTriggered(subscriptionEventFactory.call(subscriber));
            }
        });
    }

    public ContentSource(final Throwable error) {
        super(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                subscriber.onError(error);
            }
        });
    }

    /**
     * If this source emits {@link ByteBuf} or {@link ByteBufHolder} then using this operator will release the buffer
     * after it is emitted from this source.
     *
     * @return A new instance of the stream with auto-release enabled.
     */
    public Observable<T> autoRelease() {
        return this.lift(new AutoReleaseOperator<T>());
    }

    /**
     * This provides a replayable content source that only subscribes once to the actual content and then caches it,
     * till {@link DisposableContentSource#dispose()} is called.
     *
     * @return A new replayable content source.
     */
    public DisposableContentSource<T> replayable() {
        return DisposableContentSource.createNew(this);
    }

    public <R> ContentSource<R> transform(Transformer<T, R> transformer) {
        return new ContentSource<>(transformer.call(this));
    }
}
