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
package io.reactivex.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import rx.Observable;
import rx.Observable.Transformer;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action2;
import rx.functions.Func0;

/**
 * An {@link Observable.Transformer} to collect a stream of {@link ByteBuf ByteBufs} into a single
 * ByteBuf. On success the receiver must release the returned ByteBuf.
 * On failure this will release all received ByteBufs.
 * <p>
 * This Transformer should not be used with {@link io.reactivex.netty.channel.ContentSource#autoRelease()}
 * as this will release the underlying collected ByteBufs before the collection is complete.
 */
public class CollectBytes implements Transformer<ByteBuf, ByteBuf> {

    private final int maxBytes;

    /**
     * Collect all emitted ByteBufs into a single ByteBuf. This will return at most
     * {@link Integer#MAX_VALUE}
     * bytes. This is the upper limit of {@link ByteBuf#readableBytes()}. If more than
     * Integer#MAX_VALUE bytes are received a {@link TooMuchDataException} will be emitted.
     * {@link TooMuchDataException#getCause()}
     * will contain an
     * {@link OnErrorThrowable.OnNextValue} with the bytes accumulated before the exception
     * was thrown.
     */
    public static CollectBytes all() {
        return upTo(Integer.MAX_VALUE);
    }

    /**
     * Collect all emitted ByteBufs into a single ByteBuf until maxBytes have
     * been collected. If more than maxBytes are received this will unsubscribe from
     * the upstream Observable and will emit a
     * {@link TooMuchDataException}. {@link TooMuchDataException#getCause()}
     * will contain an
     * {@link OnErrorThrowable.OnNextValue} with the bytes accumulated before the exception
     * was thrown.
     * @param maxBytes the maximum number of bytes to read
     * @throws IllegalArgumentException when maxBytes is negative
     */
    public static CollectBytes upTo(int maxBytes) {
        return new CollectBytes(maxBytes);
    }

    private CollectBytes(int maxBytes) {
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must not be negative");
        }
        this.maxBytes = maxBytes;
    }

    @Override
    public Observable<ByteBuf> call(Observable<ByteBuf> upstream) {
        return upstream
            .collect(
                new Func0<CompositeByteBuf>() {
                    @Override
                    public CompositeByteBuf call() {
                        return Unpooled.compositeBuffer();
                    }
                },
                new Action2<CompositeByteBuf, ByteBuf>() {
                    @Override
                    public void call(CompositeByteBuf collector, ByteBuf buf) {
                        long newLength = collector.readableBytes() + buf.readableBytes();
                        if (newLength <= maxBytes) {
                            collector.addComponent(true, buf);
                        } else {
                            collector.release();
                            buf.release();
                            throw new TooMuchDataException("More than " + maxBytes + "B received");
                        }
                    }
                }
            )
            .cast(ByteBuf.class);
    }

    public static class TooMuchDataException extends RuntimeException {
        public TooMuchDataException(String message) {
            super(message);
        }
    }
}
