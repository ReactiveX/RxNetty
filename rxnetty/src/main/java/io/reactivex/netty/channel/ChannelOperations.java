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
package io.reactivex.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.FileRegion;
import rx.Observable;
import rx.functions.Func1;

/**
 * A list of user initiated operations that can be done on a channel.
 *
 * @param <W> Type of data that can be written on the associated channel.
 *
 * @author Nitesh Kant
 */
public interface ChannelOperations<W> {

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel.
     *
     * <h2>Flush.</h2>
     *
     * This method does not flush the write and requires an explicit {@link #flush()} call later.
     *
     * @param msg Message to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel. This {@link Observable} will <b>NOT</b> complete unless {@link #flush()}
     * is called.
     */
    Observable<Void> write(W msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush.</h2>
     *
     * This method does not flush the write and requires an explicit {@link #flush()} call later.
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel. This {@link Observable} will <b>NOT</b> complete unless {@link #flush()}
     * is called.
     */
    Observable<Void> write(Observable<W> msgs);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel.
     *
     * <h2>Flush.</h2>
     *
     * This method does not flush the write and requires an explicit {@link #flush()} call later.
     *
     * @param msg Message to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel. This {@link Observable} will <b>NOT</b> complete unless {@link #flush()}
     * is called.
     */
    Observable<Void> writeBytes(ByteBuf msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel.
     * This is equivalent to calling:
     *
     * <PRE>
     *  writeBytes(getAllocator().buffer(msg.length).writeBytes(msg));
     * </PRE>
     *
     * <h2>Flush.</h2>
     *
     * This method does not flush the write and requires an explicit {@link #flush()} call later.
     *
     * @param msg Message to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel. This {@link Observable} will <b>NOT</b> complete unless {@link #flush()}
     * is called.
     */
    Observable<Void> writeBytes(byte[] msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel.
     * This is equivalent to calling:
     *
     * <PRE>
     *  writeBytes(getAllocator().buffer(msg.length).writeBytes(msg.getBytes()));
     * </PRE>
     *
     * <h2>Flush.</h2>
     *
     * This method does not flush the write and requires an explicit {@link #flush()} call later.
     *
     * @param msg Message to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel. This {@link Observable} will <b>NOT</b> complete unless {@link #flush()}
     * is called.
     */
    Observable<Void> writeString(String msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed {@link FileRegion} on the underneath
     * channel.
     *
     * <h2>Flush.</h2>
     *
     * This method does not flush the write and requires an explicit {@link #flush()} call later.
     *
     * @param region File region to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel. This {@link Observable} will <b>NOT</b> complete unless {@link #flush()}
     * is called.
     */
    Observable<Void> writeFileRegion(FileRegion region);

    /**
     * On subscription of the returned {@link Observable}, flushes any writes performed on the channel before the
     * subscription.
     *
     * @return An {@link Observable} representing the result of all writes happened prior to the flush. Every
     * subscription to this {@link Observable} will flush all pending writes.
     */
    Observable<Void> flush();

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel and
     * flushes the channel. Any writes issued before subscribing, will also be flushed. However, the returned
     * {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this write
     * does not, the returned {@link Observable} will not fail.
     *
     * @param msg Message to write.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed message and flush all pending writes.
     */
    Observable<Void> writeAndFlush(W msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
     * and flushes the channel. Any writes issued before subscribing, will also be flushed. However, the returned
     * {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this write
     * does not, the returned {@link Observable} will not fail.
     *
     * @param msgs Message stream to write.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed message and flush all pending writes.
     */
    Observable<Void> writeAndFlush(Observable<W> msgs);

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
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
     * {@code flushSelector} returns {@code true}
     */
    Observable<Void> writeAndFlush(Observable<W> msgs, Func1<W, Boolean> flushSelector);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
     * and flushes the channel, on every write. Any writes issued before subscribing, will also be flushed. However, the
     * returned {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this
     * write does not, the returned {@link Observable} will not fail.
     *
     * @param msgs Message stream to write.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed messages and flush all pending writes, on every
     * write.
     */
    Observable<Void> writeAndFlushOnEach(Observable<W> msgs);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel and
     * flushes the channel. Any writes issued before subscribing, will also be flushed. However, the returned
     * {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this write
     * does not, the returned {@link Observable} will not fail.
     *
     * @param msg Message to write.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed message and flush all pending writes.
     */
    Observable<Void> writeBytesAndFlush(ByteBuf msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel and
     * flushes the channel. Any writes issued before subscribing, will also be flushed. However, the returned
     * {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this write
     * does not, the returned {@link Observable} will not fail. This is equivalent to calling:
     *
     * <PRE>
     *  writeBytesAndFlush(getAllocator().buffer(msg.length).writeBytes(msg));
     * </PRE>
     *
     * @param msg Message to write.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed message and flush all pending writes.
     */
    Observable<Void> writeBytesAndFlush(byte[] msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message on the underneath channel and
     * flushes the channel. Any writes issued before subscribing, will also be flushed. However, the returned
     * {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this write
     * does not, the returned {@link Observable} will not fail. This is equivalent to calling:
     *
     * <PRE>
     *  writeBytesAndFlush(getAllocator().buffer(msg.length).writeBytes(msg.getBytes()));
     * </PRE>
     *
     * @param msg Message to write.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed message and flush all pending writes.
     */
    Observable<Void> writeStringAndFlush(String msg);

    /**
     * On subscription of the returned {@link Observable}, writes the passed file region on the underneath channel and
     * flushes the channel. Any writes issued before subscribing, will also be flushed. However, the returned
     * {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this write
     * does not, the returned {@link Observable} will not fail.
     *
     * @param fileRegion File region to write.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed region and flush all pending writes.
     */
    Observable<Void> writeFileRegionAndFlush(FileRegion fileRegion);

    /**
     * Cancels all writes which have not been flushed till now.
     *
     * @param mayInterruptIfRunning If the thread has to be interrupted upon cancelling.
     */
    void cancelPendingWrites(boolean mayInterruptIfRunning);

    /**
     * Returns {@link ByteBufAllocator} to be used for creating {@link ByteBuf}
     *
     * @return {@link ByteBufAllocator}
     */
    ByteBufAllocator getAllocator();

    /**
     * Flushes any pending writes and closes the connection. Same as calling {@code close(true)}
     *
     * @return {@link Observable} representing the result of close.
     */
    Observable<Void> close();

    /**
     * Closes this channel after flushing all pending writes.
     *
     * @return {@link Observable} representing the result of close and flush.
     */
    Observable<Void> close(boolean flush);

}
