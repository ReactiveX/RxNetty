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

package io.reactivex.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.ReferenceCountUtil;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Similar to {@link ContentSource} but supports multicast to multiple subscriptions. This source, subscribes upstream
 * once and then caches the content, till the time {@link #dispose()} is called.
 *
 * <h2>Managing {@link ByteBuf} lifecycle.</h2>
 *
 * If this source emits {@link ByteBuf} or a {@link ByteBufHolder}, using {@link #autoRelease()} will release the buffer
 * after emitting it from this source.
 *
 * Every subscriber to this source must manage it's own lifecycle of the items it receives i.e. the buffers must be
 * released by every subscriber post processing.
 *
 * <h2>Disposing the source</h2>
 *
 * It is mandatory to call {@link #dispose()} on this source when no more subscriptions are required. Failure to do so,
 * will cause a buffer leak as this source, caches the contents till disposed.
 *
 * Typically, {@link #dispose()} can be called as an {@link Subscriber#unsubscribe()} action.
 *
 * @param <T> Type of objects emitted by this source.
 */
public final class DisposableContentSource<T> extends Observable<T> {

    private final OnSubscribeImpl<T> onSubscribe;

    private DisposableContentSource(final OnSubscribeImpl<T> onSubscribe) {
        super(onSubscribe);
        this.onSubscribe = onSubscribe;
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
     * Disposes this source.
     */
    public void dispose() {
        if (onSubscribe.disposed.compareAndSet(false, true)) {
            for (Object chunk : onSubscribe.chunks) {
                ReferenceCountUtil.release(chunk);
            }
            onSubscribe.chunks.clear();
        }
    }

    static <X> DisposableContentSource<X> createNew(Observable<X> source) {
        final ArrayList<X> chunks = new ArrayList<>();
        ConnectableObservable<X> replay = source.doOnNext(new Action1<X>() {
            @Override
            public void call(X x) {
                chunks.add(x);
            }
        }).replay();
        return new DisposableContentSource<>(new OnSubscribeImpl<X>(replay, chunks));
    }

    private static class OnSubscribeImpl<T> implements OnSubscribe<T> {

        private final ConnectableObservable<T> source;
        private final ArrayList<T> chunks;
        private boolean subscribed;
        private final AtomicBoolean disposed = new AtomicBoolean();

        public OnSubscribeImpl(ConnectableObservable<T> source, ArrayList<T> chunks) {
            this.source = source;
            this.chunks = chunks;
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {

            if (disposed.get()) {
                subscriber.onError(new IllegalStateException("Content source is already disposed."));
            }

            boolean connectNow = false;

            synchronized (this) {
                if (!subscribed) {
                    connectNow = true;
                    subscribed = true;
                }
            }

            source.doOnNext(new Action1<T>() {
                @Override
                public void call(T msg) {
                    ReferenceCountUtil.retain(msg);
                }
            }).unsafeSubscribe(subscriber);

            if (connectNow) {
                source.connect();
            }
        }
    }
}
