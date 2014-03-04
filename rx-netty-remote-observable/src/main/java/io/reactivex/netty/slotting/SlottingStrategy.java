package io.reactivex.netty.slotting;

import io.reactivex.netty.RemoteRxEvent;
import io.reactivex.netty.channel.ObservableConnection;
import rx.functions.Func1;

public interface SlottingStrategy<T> {

	public SlotAssignment assignSlot(ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection);
	public void releaseSlot(ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection);
	public Func1<SlotValuePair<T>,Boolean> slottingFunction();
}
