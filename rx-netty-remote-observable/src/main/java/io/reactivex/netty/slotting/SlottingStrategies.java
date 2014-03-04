package io.reactivex.netty.slotting;

public class SlottingStrategies {

	private SlottingStrategies(){}
	
	public static <T> SlottingStrategy<T> noSlotting(){
		return new NoSlotting<T>();
	}
	
	public static <T> SlottingStrategy<T> hashCodeSlotting(int numSlots){
		return new HashCodeSlotting<T>(numSlots);
	}
}
