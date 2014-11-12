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

public class SlotAssignment {

	private boolean assigned;
	private Integer slotAssignment;
	private int numSlots;
	
	private SlotAssignment() {}
	
	public static SlotAssignment notAssigned(){
		return new SlotAssignment();
	}
	
	public SlotAssignment(Integer slotAssignment, int numSlots) {
		this.slotAssignment = slotAssignment;
		this.numSlots = numSlots;
		this.assigned = true;
	}

	public Integer getSlotAssignment() {
		return slotAssignment;
	}

	public int getNumSlots() {
		return numSlots;
	}

	public boolean isAssigned() {
		return assigned;
	}
}
