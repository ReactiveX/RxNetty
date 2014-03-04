package io.reactivex.netty.codec;

public interface Encoder<T> {

	public byte[] encode(T value);
}
