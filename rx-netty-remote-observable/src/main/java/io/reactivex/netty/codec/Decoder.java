package io.reactivex.netty.codec;

public interface Decoder<T> {

	public T decode(byte[] bytes);
	
}
