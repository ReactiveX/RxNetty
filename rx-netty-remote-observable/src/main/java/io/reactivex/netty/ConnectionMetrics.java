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
