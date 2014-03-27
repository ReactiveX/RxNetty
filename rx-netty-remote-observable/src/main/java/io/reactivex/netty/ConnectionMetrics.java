package io.reactivex.netty;

import com.netflix.numerus.NumerusProperty;
import com.netflix.numerus.NumerusRollingNumber;

public class ConnectionMetrics {

	private NumerusRollingNumber dataReceivedFromServerCounter 
		= new NumerusRollingNumber(RemoteRxNumerusEvent.BOOTSTRAP, 
				NumerusProperty.Factory.asProperty(1000), 
				NumerusProperty.Factory.asProperty(10));
	
	void incrementNextCount(){
		dataReceivedFromServerCounter.increment(RemoteRxNumerusEvent.NEXT);
	}
	
	void incrementErrorCount(){
		dataReceivedFromServerCounter.increment(RemoteRxNumerusEvent.ERROR);
	}
	
	void incrementCompletedCount(){
		dataReceivedFromServerCounter.increment(RemoteRxNumerusEvent.COMPLETED);
	}

	public long getOnNextCount(){
		return dataReceivedFromServerCounter
				.getCumulativeSum(RemoteRxNumerusEvent.NEXT);
	}
	
	public long getOnErrorCount(){
		return dataReceivedFromServerCounter
				.getCumulativeSum(RemoteRxNumerusEvent.ERROR);
	}
	
	public long getOnCompletedCount(){
		return dataReceivedFromServerCounter
				.getCumulativeSum(RemoteRxNumerusEvent.COMPLETED);
	}
	
}
