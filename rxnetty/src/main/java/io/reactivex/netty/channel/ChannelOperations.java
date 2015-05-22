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

import io.netty.channel.Channel;
import io.netty.channel.FileRegion;
import io.netty.util.AttributeKey;
import rx.Observable;
import rx.functions.Func1;

/**
 * A list of user initiated operations that can be done on a channel.
 *
 * @param <W> Type of data that can be written on the associated channel.
 */
public interface ChannelOperations<W> {

    /**
     * Flush selector that always returns true.
     */
    Func1<String, Boolean> FLUSH_ON_EACH_STRING = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String next) {
            return true;
        }
    };

    /**
     * Flush selector that always returns true.
     */
    Func1<byte[], Boolean> FLUSH_ON_EACH_BYTES = new Func1<byte[], Boolean>() {
        @Override
        public Boolean call(byte[] next) {
            return true;
        }
    };

    /**
     * Flush selector that always returns true.
     */
    Func1<FileRegion, Boolean> FLUSH_ON_EACH_FILE_REGION = new Func1<FileRegion, Boolean>() {
        @Override
        public Boolean call(FileRegion next) {
            return true;
        }
    };
    AttributeKey<Boolean> FLUSH_ONLY_ON_READ_COMPLETE =
            AttributeKey.valueOf("_rxnetyy-flush-only-on-read-complete");

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush.</h2>
     *
     * All writes will be flushed on completion of the passed {@code Observable}
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    Observable<Void> write(Observable<W> msgs);

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
    Observable<Void> write(Observable<W> msgs, Func1<W, Boolean> flushSelector);

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
    Observable<Void> writeAndFlushOnEach(Observable<W> msgs);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush.</h2>
     *
     * All writes will be flushed on completion of the passed {@code Observable}
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    Observable<Void> writeString(Observable<String> msgs);

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
    Observable<Void> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector);

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
    Observable<Void> writeStringAndFlushOnEach(Observable<String> msgs);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush.</h2>
     *
     * All writes will be flushed on completion of the passed {@code Observable}
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    Observable<Void> writeBytes(Observable<byte[]> msgs);

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
    Observable<Void> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector);

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
    Observable<Void> writeBytesAndFlushOnEach(Observable<byte[]> msgs);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush.</h2>
     *
     * All writes will be flushed on completion of the passed {@code Observable}
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    Observable<Void> writeFileRegion(Observable<FileRegion> msgs);

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
    Observable<Void> writeFileRegion(Observable<FileRegion> msgs, Func1<FileRegion, Boolean> flushSelector);

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
    Observable<Void> writeFileRegionAndFlushOnEach(Observable<FileRegion> msgs);

    /**
     * Flushes any pending writes on this connection by calling {@link Channel#flush()}. This can be used for
     * implementing any custom flusing strategies that otherwise can not be implemented by methods like
     * {@link #write(Observable, Func1)}.
     */
    void flush();

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

    /**
     * Closes the connection immediately. Same as calling {@link #close()} and subscribing to the returned
     * {@code Observable}
     */
    void closeNow();
}
