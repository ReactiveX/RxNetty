package io.reactivex.netty.slotting;

import io.reactivex.netty.RemoteRxEvent;
import io.reactivex.netty.channel.ObservableConnection;
import rx.functions.Func1;

public class NoSlotting<T> implements SlottingStrategy<T> {

	NoSlotting(){}
	
	@Override
	public SlotAssignment assignSlot(ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection) {
		return new SlotAssignment(1, 1);
	}

	@Override
	public void releaseSlot(ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {}

	@Override
	public Func1<SlotValuePair<T>, Boolean> slottingFunction() {
		return new Func1<SlotValuePair<T>,Boolean>(){
			@Override
			public Boolean call(SlotValuePair<T> t1) {
				return true;
			}
		};
	}

}
