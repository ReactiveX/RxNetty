package io.reactivex.netty;

public class MutableReference<T> {

	private T value;

	public T getValue() {
		return value;
	}
	public void setValue(T value) {
		this.value = value;
	}
}
