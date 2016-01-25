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

package io.reactivex.netty.protocol.http.server;

import io.reactivex.netty.protocol.http.TrailingHeaders;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

class FailedContentWriter<C> extends ResponseContentWriter<C> {

    FailedContentWriter() {
        super(new OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                subscriber.onError(new IllegalStateException("HTTP headers are already sent."));
            }
        });
    }

    @Override
    public ResponseContentWriter<C> write(Observable<C> msgs) {
        return this;
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource,
                                                              Func0<T> trailerFactory,
                                                              Func2<T, C, T> trailerMutator) {
        return this;
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource, Func0<T> trailerFactory,
                                                              Func2<T, C, T> trailerMutator,
                                                              Func1<C, Boolean> flushSelector) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> write(Observable<C> msgs, Func1<C, Boolean> flushSelector) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> writeAndFlushOnEach(Observable<C> msgs) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> writeString(Observable<String> msgs) {
        return this;
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                    Func0<T> trailerFactory,
                                                                    Func2<T, String, T> trailerMutator) {
        return this;
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                    Func0<T> trailerFactory,
                                                                    Func2<T, String, T> trailerMutator,
                                                                    Func1<String, Boolean> flushSelector) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> writeStringAndFlushOnEach(Observable<String> msgs) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs) {
        return this;
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                   Func0<T> trailerFactory,
                                                                   Func2<T, byte[], T> trailerMutator) {
        return this;
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                   Func0<T> trailerFactory,
                                                                   Func2<T, byte[], T> trailerMutator,
                                                                   Func1<byte[], Boolean> flushSelector) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector) {
        return this;
    }

    @Override
    public ResponseContentWriter<C> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
        return this;
    }
}
