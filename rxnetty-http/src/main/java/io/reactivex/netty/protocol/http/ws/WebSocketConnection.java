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
package io.reactivex.netty.protocol.http.ws;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.annotations.Beta;
import rx.functions.Func1;

/**
 * A WebSocket connection which is used to read/write {@link WebSocketFrame}s.
 */
public final class WebSocketConnection {

    private final Connection<WebSocketFrame, WebSocketFrame> delegate;

    public WebSocketConnection(Connection<WebSocketFrame, WebSocketFrame> delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the input stream for this connection, until a {@link CloseWebSocketFrame} is received. The terminal
     * {@link CloseWebSocketFrame} is included in the returned stream.
     *
     * @return The input stream for this connection.
     */
    public Observable<WebSocketFrame> getInput() {
        return getInput(true);
    }

    /**
     * Returns the input stream for this connection. If {@code untilCloseFrame} is {@code true} then the returned stream
     * completes after receiving (and emitting) a {@link CloseWebSocketFrame}, otherwise, it completes with an error
     * when the underlying channel is closed.
     *
     * @return The input stream for this connection.
     */
    @Beta
    public Observable<WebSocketFrame> getInput(boolean untilCloseFrame) {
        Observable<WebSocketFrame> rawInput = Observable.create(new OnSubscribe<WebSocketFrame>() {
            @Override
            public void call(Subscriber<? super WebSocketFrame> subscriber) {
                delegate.unsafeNettyChannel().pipeline()
                        .fireUserEventTriggered(new HttpContentSubscriberEvent<>(subscriber));
            }
        });
        if (untilCloseFrame) {
            return rawInput.takeUntil(new Func1<WebSocketFrame, Boolean>() {
                @Override
                public Boolean call(WebSocketFrame webSocketFrame) {
                    return webSocketFrame instanceof CloseWebSocketFrame;
                }
            });
        } else {
            return rawInput;
        }
    }

    /**
     * Writes a stream of frames on this connection. The writes are flushed on completion of the stream, if other flush
     * strategies are required, one must use {@link #write(Observable, Func1)} or
     * {@link #writeAndFlushOnEach(Observable)}
     *
     * @param msgs Stream of frames to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    public Observable<Void> write(Observable<WebSocketFrame> msgs) {
        return delegate.write(msgs);
    }

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
     * and flushes the channel, everytime, {@code flushSelector} returns {@code true} . Any writes issued before
     * subscribing, will also be flushed. However, the returned {@link Observable} will not capture the result of those
     * writes, i.e. if the other writes, fail and this write does not, the returned {@link Observable} will not fail.
     *
     * @param msgs Message stream to write.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. Channel is
     * flushed, iff this function returns, {@code true}.
     *
     * @return An {@link Observable} representing the result of this write. Every
     * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
     * {@code flushSelector} returns {@code true}
     */
    public Observable<Void> write(Observable<WebSocketFrame> msgs, Func1<WebSocketFrame, Boolean> flushSelector) {
        return delegate.write(msgs, flushSelector);
    }

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
     * and flushes the channel, on every write. Any writes issued before subscribing, will also be flushed. However, the
     * returned {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this
     * write does not, the returned {@link Observable} will not fail.
     *
     * @param msgs Message stream to write.
     *
     * @return An {@link Observable} representing the result of this write. Every
     * subscription to this {@link Observable} will write the passed messages and flush all pending writes, on every
     * write.
     */
    public Observable<Void> writeAndFlushOnEach(Observable<WebSocketFrame> msgs) {
        return delegate.writeAndFlushOnEach(msgs);
    }

    /**
     * Flushes all writes, if any, before calling the flush.
     */
    public void flush() {
        delegate.flush();
    }

    /**
     * Flushes any pending writes and closes the connection. Same as calling {@code close(true)}
     *
     * @return {@link Observable} representing the result of close.
     */
    public Observable<Void> close() {
        return delegate.close();
    }

    /**
     * Closes this channel after flushing all pending writes.
     *
     * @return {@link Observable} representing the result of close and flush.
     */
    public Observable<Void> close(boolean flush) {
        return delegate.close(flush);
    }

    /**
     * Returns an {@link Observable} that completes when this connection is closed.
     *
     * @return An {@link Observable} that completes when this connection is closed.
     */
    public Observable<Void> closeListener() {
        return delegate.closeListener();
    }

    /**
     * Closes the connection immediately. Same as calling {@link #close()} and subscribing to the returned
     * {@code Observable}
     */
    public void closeNow() {
        delegate.closeNow();
    }
}
