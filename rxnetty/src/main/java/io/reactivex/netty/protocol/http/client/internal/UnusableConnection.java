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
 */
package io.reactivex.netty.protocol.http.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.FileRegion;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.events.EventPublisher;
import rx.Observable;
import rx.functions.Func1;

final class UnusableConnection<R, W> extends Connection<R, W> {

    protected UnusableConnection(Channel nettyChannel,
                                 ConnectionEventListener eventListener,
                                 EventPublisher eventPublisher) {
        super(nettyChannel, eventListener, eventPublisher);
    }

    @Override
    public Observable<Void> write(Observable<W> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> write(Observable<W> msgs, Func1<W, Boolean> flushSelector) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeAndFlushOnEach(Observable<W> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeStringAndFlushOnEach(Observable<String> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs, Func1<FileRegion, Boolean> flushSelector) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> writeFileRegionAndFlushOnEach(Observable<FileRegion> msgs) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public void flush() {
        throw new IllegalStateException("Connection is not usable.");
    }

    @Override
    public Observable<Void> close() {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public Observable<Void> close(boolean flush) {
        return Observable.error(new IllegalStateException("Connection is not usable."));
    }

    @Override
    public void closeNow() {
        throw new IllegalStateException("Connection is not usable.");
    }

    public static Connection<?, ?> create() {
        return new UnusableConnection<>(new EmbeddedChannel(), null, null);
    }
}
