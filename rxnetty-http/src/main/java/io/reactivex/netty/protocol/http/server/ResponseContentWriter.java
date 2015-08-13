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
package io.reactivex.netty.protocol.http.server;

import io.reactivex.netty.protocol.http.TrailingHeaders;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * A facility to optionally write content to the response.
 *
 * <h2>Thread safety</h2>
 *
 * This object is not thread-safe and can not be accessed from multiple threads.
 */
public abstract class ResponseContentWriter<C> extends Observable<Void> {

    ResponseContentWriter(OnSubscribe<Void> f) {
        super(f);
    }

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush</h2>
     *
     * The writes are flushed when the passed stream completes.
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    public abstract ResponseContentWriter<C> write(Observable<C> msgs);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * <h2>Multiple invocations</h2>
     *
     * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
     * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
     * So, any subsequent invocation of this method will always emit an error when subscribed.
     *
     * <h2>Flush</h2>
     *
     * The writes are flushed when the passed stream completes.
     *
     * @param contentSource Content source for the response.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource,
                                                                       Func0<T> trailerFactory,
                                                                       Func2<T, C, T> trailerMutator);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * <h2>Multiple invocations</h2>
     *
     * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
     * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
     * So, any subsequent invocation of this method will always emit an error when subscribed.
     *
     * @param contentSource Content source for the response.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. Channel is
     * flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource,
                                                                       Func0<T> trailerFactory,
                                                                       Func2<T, C, T> trailerMutator,
                                                                       Func1<C, Boolean> flushSelector);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
     * and flushes the channel, everytime, {@code flushSelector} returns {@code true} . Any writes issued before
     * subscribing, will also be flushed. However, the returned {@link Observable} will not capture the result of those
     * writes, i.e. if the other writes, fail and this write does not, the returned {@link Observable} will not fail.
     *
     * @param msgs Message stream to write.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}.
     * Channel is flushed, iff this function returns, {@code true}.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
     * {@code flushSelector} returns {@code true}
     */
    public abstract ResponseContentWriter<C> write(Observable<C> msgs, Func1<C, Boolean> flushSelector);

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
    public abstract ResponseContentWriter<C> writeAndFlushOnEach(Observable<C> msgs);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush</h2>
     *
     * The writes are flushed when the passed stream completes.
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    public abstract ResponseContentWriter<C> writeString(Observable<String> msgs);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * <h2>Multiple invocations</h2>
     *
     * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
     * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
     * So, any subsequent invocation of this method will always emit an error when subscribed.
     *
     * <h2>Flush</h2>
     *
     * The writes are flushed when the passed stream completes.
     *
     * @param contentSource Content source for the response.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                             Func0<T> trailerFactory,
                                                                             Func2<T, String, T> trailerMutator);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * <h2>Multiple invocations</h2>
     *
     * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
     * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
     * So, any subsequent invocation of this method will always emit an error when subscribed.
     *
     * @param contentSource Content source for the response.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. Channel is
     * flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                             Func0<T> trailerFactory,
                                                                             Func2<T, String, T> trailerMutator,
                                                                             Func1<String, Boolean> flushSelector);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
     * and flushes the channel, everytime, {@code flushSelector} returns {@code true} . Any writes issued before
     * subscribing, will also be flushed. However, the returned {@link Observable} will not capture the result of those
     * writes, i.e. if the other writes, fail and this write does not, the returned {@link Observable} will not fail.
     *
     * @param msgs Message stream to write.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}.
     * Channel is flushed, iff this function returns, {@code true}.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
     * {@code flushSelector} returns {@code true}
     */
    public abstract ResponseContentWriter<C> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector);

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
    public abstract ResponseContentWriter<C> writeStringAndFlushOnEach(Observable<String> msgs);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
     *
     * <h2>Flush</h2>
     *
     * The writes are flushed when the passed stream completes.
     *
     * @param msgs Stream of messages to write.
     *
     * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
     * will replay the write on the channel.
     */
    public abstract ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * <h2>Multiple invocations</h2>
     *
     * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
     * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
     * So, any subsequent invocation of this method will always emit an error when subscribed.
     *
     * <h2>Flush</h2>
     *
     * The writes are flushed when the passed stream completes.
     *
     * @param contentSource Content source for the response.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                            Func0<T> trailerFactory,
                                                                            Func2<T, byte[], T> trailerMutator);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * <h2>Multiple invocations</h2>
     *
     * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
     * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
     * So, any subsequent invocation of this method will always emit an error when subscribed.
     *
     * @param contentSource Content source for the response.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. Channel is
     * flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                            Func0<T> trailerFactory,
                                                                            Func2<T, byte[], T> trailerMutator,
                                                                            Func1<byte[], Boolean> flushSelector);

    /**
     * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
     * and flushes the channel, everytime, {@code flushSelector} returns {@code true} . Any writes issued before
     * subscribing, will also be flushed. However, the returned {@link Observable} will not capture the result of those
     * writes, i.e. if the other writes, fail and this write does not, the returned {@link Observable} will not fail.
     *
     * @param msgs Message stream to write.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}.
     * Channel is flushed, iff this function returns, {@code true}.
     *
     * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
     * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
     * {@code flushSelector} returns {@code true}
     */
    public abstract ResponseContentWriter<C> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector);

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
    public abstract ResponseContentWriter<C> writeBytesAndFlushOnEach(Observable<byte[]> msgs);
}
