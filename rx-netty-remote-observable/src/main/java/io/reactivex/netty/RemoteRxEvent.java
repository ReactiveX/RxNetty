package io.reactivex.netty;

import java.util.Map;


public class RemoteRxEvent {

	private String observableName;
	public enum Type {next,completed,error,subscribed,unsubscribed}
	private Type type;
	private byte[] data;
	private Map<String,String> subscriptionParameters;
	
	public RemoteRxEvent(String observableName,
			Type type, byte[] data, Map<String,String> subscriptionParameters) {
		this.observableName = observableName;
		this.type = type;
		this.data = data;
		this.subscriptionParameters = subscriptionParameters;
	}
	public byte[] getData() {
		return data;
	}
	public Type getType() {
		return type;
	}
	public String getObservableName() {
		return observableName;
	}
	Map<String,String> getSubscribeParameters() {
		return subscriptionParameters;
	}
	public static RemoteRxEvent next(String observableName, byte[] nextData){
		return new RemoteRxEvent(observableName, Type.next, nextData, null);
	}
	public static RemoteRxEvent completed(String observableName){
		return new RemoteRxEvent(observableName, Type.completed, null, null);
	}
	public static RemoteRxEvent error(String observableName, byte[] errorData){
		return new RemoteRxEvent(observableName, Type.error, errorData, null);
	}
	public static RemoteRxEvent subscribed(String observableName, Map<String,String> subscriptionParameters){
		return new RemoteRxEvent(observableName, Type.subscribed, null, subscriptionParameters);
	}
	public static RemoteRxEvent unsubscribed(String observableName){
		return new RemoteRxEvent(observableName, Type.unsubscribed, null, null);
	}
}
