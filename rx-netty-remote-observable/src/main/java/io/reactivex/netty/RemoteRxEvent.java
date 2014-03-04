package io.reactivex.netty;


public class RemoteRxEvent {

	public enum Type {next,completed,error,subscribed,unsubscribed}
	private byte[] data;
	private Type type;
	
	RemoteRxEvent(byte[] data, Type type) {
		this.data = data;
		this.type = type;
	}
	public byte[] getData() {
		return data;
	}
	public Type getType() {
		return type;
	}
	public static RemoteRxEvent next(byte[] nextData){
		return new RemoteRxEvent(nextData, Type.next);
	}
	public static RemoteRxEvent completed(){
		return new RemoteRxEvent(null, Type.completed);
	}
	public static RemoteRxEvent error(byte[] errorData){
		return new RemoteRxEvent(errorData, Type.error);
	}
	public static RemoteRxEvent subscribed(){
		return new RemoteRxEvent(null, Type.subscribed);
	}
	public static RemoteRxEvent unsubscribed(){
		return new RemoteRxEvent(null, Type.unsubscribed);
	}
}
