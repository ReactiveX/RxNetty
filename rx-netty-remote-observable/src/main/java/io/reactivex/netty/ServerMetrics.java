package io.reactivex.netty;

import com.netflix.numerus.NumerusProperty;
import com.netflix.numerus.NumerusRollingNumber;

public class ServerMetrics {

	private NumerusRollingNumber dataPushedToClientsCounter 
	= new NumerusRollingNumber(RemoteRxNumerusEvent.BOOTSTRAP, 
			NumerusProperty.Factory.asProperty(1000), 
			NumerusProperty.Factory.asProperty(10));
	
	void incrementNextCount(){
		dataPushedToClientsCounter.increment(RemoteRxNumerusEvent.NEXT);
	}
	
	void incrementErrorCount(){
		dataPushedToClientsCounter.increment(RemoteRxNumerusEvent.ERROR);
	}
	
	void incrementCompletedCount(){
		dataPushedToClientsCounter.increment(RemoteRxNumerusEvent.COMPLETED);
	}
	
	void incrementSubscribedCount(){
		dataPushedToClientsCounter.increment(RemoteRxNumerusEvent.SUBSCRIBE);
	}
	
	void incrementUnsubscribedCount(){
		dataPushedToClientsCounter.increment(RemoteRxNumerusEvent.UNSUBSCRIBE);
	}

	public long getOnNextCount(){
		return dataPushedToClientsCounter
				.getCumulativeSum(RemoteRxNumerusEvent.NEXT);
	}
	
	public long getOnErrorCount(){
		return dataPushedToClientsCounter
				.getCumulativeSum(RemoteRxNumerusEvent.ERROR);
	}
	
	public long getOnCompletedCount(){
		return dataPushedToClientsCounter
				.getCumulativeSum(RemoteRxNumerusEvent.COMPLETED);
	}
	
	public long getSubscribedCount(){
		return dataPushedToClientsCounter
				.getCumulativeSum(RemoteRxNumerusEvent.SUBSCRIBE);
	}
	
	public long getUnsubscribedCount(){
		return dataPushedToClientsCounter
				.getCumulativeSum(RemoteRxNumerusEvent.UNSUBSCRIBE);
	}
	
}
