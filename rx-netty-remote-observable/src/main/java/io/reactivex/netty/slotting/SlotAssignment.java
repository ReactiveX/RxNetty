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
