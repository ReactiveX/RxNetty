package io.reactivex.netty.codec;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Codecs {
	
	public static Codec<Integer> integer(){
		
		return new Codec<Integer>(){
			@Override
			public Integer decode(byte[] bytes) {
				return ByteBuffer.wrap(bytes).getInt();
			}
			@Override
			public byte[] encode(final Integer value) {
				return ByteBuffer.allocate(4).putInt(value).array();
			}
		};
	}
	
	public static Codec<String> string(){
		return new Codec<String>(){
			@Override
			public String decode(byte[] bytes) {
				return new String(bytes, Charset.defaultCharset());
			}
			@Override
			public byte[] encode(final String value) {
				return value.getBytes(Charset.defaultCharset());
			}
		};
	}
}
