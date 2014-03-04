package io.reactivex.netty.slotting;

public class SlotValuePair<T> {

	private int slot;
	private T value;
	
	public SlotValuePair(int slot, T value) {
		this.slot = slot;
		this.value = value;
	}
	public int getSlot() {
		return slot;
	}
	public T getValue() {
		return value;
	}
}
