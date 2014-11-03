/*
 * Copyright 2014 Netflix, Inc.
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

/**
 * An interface to capture how one can write on a channel.
 *
 * @author Nitesh Kant
 */
public interface ChannelWriter<O> {

    Observable<Void> writeAndFlush(O msg);

    void write(O msg);

    <R> void write(R msg, ContentTransformer<R> transformer);

    void writeBytes(ByteBuf msg);

    void writeBytes(byte[] msg);

    void writeString(String msg);

    void writeFileRegion(FileRegion region);
    
    Observable<Void> flush();

    void cancelPendingWrites(boolean mayInterruptIfRunning);

    ByteBufAllocator getAllocator();

    <R> Observable<Void> writeAndFlush(R msg, ContentTransformer<R> transformer);

    Observable<Void> writeBytesAndFlush(ByteBuf msg);

    Observable<Void> writeBytesAndFlush(byte[] msg);

    Observable<Void> writeStringAndFlush(String msg);

    Observable<Void> close();

    Observable<Void> close(boolean flush);
    
}
