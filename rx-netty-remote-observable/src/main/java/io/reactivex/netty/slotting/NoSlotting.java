/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
