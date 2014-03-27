package io.reactivex.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.reactivex.netty.serialization.ContentTransformer;
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

    void writeBytes(byte[] msg);

    void writeString(String msg);

    Observable<Void> flush();

    void cancelPendingWrites(boolean mayInterruptIfRunning);

    ByteBufAllocator getAllocator();

    <R> Observable<Void> writeAndFlush(R msg, ContentTransformer<R> transformer);

    Observable<Void> writeBytesAndFlush(byte[] msg);

    Observable<Void> writeStringAndFlush(String msg);
}
