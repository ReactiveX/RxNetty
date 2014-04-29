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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rx.functions.Func1;

public class HashCodeSlotting<T> implements SlottingStrategy<T> {

	private int numSlots;
	private List<Integer> slotTokens = new LinkedList<Integer>();
	private Map<ObservableConnection<RemoteRxEvent, RemoteRxEvent>,Integer> slotAssignments 
		= new HashMap<ObservableConnection<RemoteRxEvent, RemoteRxEvent>,Integer>();
	
	HashCodeSlotting(int numSlots){
		this.numSlots = numSlots;
		// add tokens
		for(int i=0; i<numSlots; i++){
			slotTokens.add(i);
		}
	}
	
	@Override
	public synchronized SlotAssignment assignSlot(ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection) {
		SlotAssignment assignment = SlotAssignment.notAssigned();
		if (slotTokens.size() > 0){
			Integer slot = slotTokens.remove(0); // grab first slot
			slotAssignments.put(connection, slot); // make assignment
			assignment = new SlotAssignment(slotAssignments.get(connection), numSlots);
		}
		return assignment;
	}

	@Override
	public synchronized void releaseSlot(ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection) {
		Integer freeSlot = slotAssignments.get(connection);
		if (freeSlot != null){
			slotTokens.add(freeSlot);
			slotAssignments.remove(connection);
		}
	}

	@Override
	public Func1<SlotValuePair<T>, Boolean> slottingFunction() {
		return new Func1<SlotValuePair<T>,Boolean>(){
			@Override
			public Boolean call(SlotValuePair<T> pair) {
				T value = pair.getValue();
				int slot = pair.getSlot();
				return ((value.hashCode() & Integer.MAX_VALUE) % numSlots) == slot;
			}
		};
	}

}
