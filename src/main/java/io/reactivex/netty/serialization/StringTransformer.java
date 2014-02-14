package io.reactivex.netty.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public class StringTransformer implements ContentTransformer<String> {

    private final Charset charset;

    public StringTransformer() {
        this(Charset.defaultCharset());
    }

    public StringTransformer(Charset charset) {
        this.charset = charset;
    }

    @Override
    public ByteBuf transform(String toTransform, ByteBufAllocator byteBufAllocator) {
        byte[] contentAsBytes = toTransform.getBytes(charset);
        return byteBufAllocator.buffer(contentAsBytes.length).writeBytes(contentAsBytes);
    }
}
