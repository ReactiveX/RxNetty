package io.reactivex.netty.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * @author Nitesh Kant
 */
public class ByteTransformer implements ContentTransformer<byte[]> {

    @Override
    public ByteBuf transform(byte[] toTransform, ByteBufAllocator byteBufAllocator) {
        return byteBufAllocator.buffer(toTransform.length).writeBytes(toTransform);
    }
}
