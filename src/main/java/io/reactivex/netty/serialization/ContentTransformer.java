package io.reactivex.netty.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * @author Nitesh Kant
 */
public interface ContentTransformer<T> {

    ByteBuf transform(T toTransform, ByteBufAllocator byteBufAllocator);
}
