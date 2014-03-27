package io.reactivex.netty;

import java.util.Map;

import io.reactivex.netty.codec.Decoder;

public class ConnectParams<T> {

	private String host;
	private int port;
	private String name;
	private Decoder<T> decoder;
	private Map<String,String> subscribeParameters;
	
	ConnectParams(Builder<T> builder){
		this.host = builder.host;
		this.name = builder.name;
		this.port = builder.port;
		this.decoder = builder.decoder;
		this.subscribeParameters = builder.subscribeParameters;
	}
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getName() {
		return name;
	}

	public Decoder<T> getDecoder() {
		return decoder;
	}

	public Map<String, String> getSubscribeParameters() {
		return subscribeParameters;
	}

	public static class Builder<T>{
		
		private String host;
		private int port;
		private String name;
		private Decoder<T> decoder;
		private Map<String,String> subscribeParameters;
		
		public Builder<T> host(String host){
			this.host = host;
			return this;
		}
		
		public Builder<T> port(int port){
			this.port = port;
			return this;
		}
		
		public Builder<T> name(String name){
			this.name = name;
			return this;
		}
		
		public Builder<T> decoder(Decoder<T> decoder){
			this.decoder = decoder;
			return this;
		}
		
		public Builder<T> subscribeParameters(Map<String,String> subscribeParameters){
			this.subscribeParameters = subscribeParameters;
			return this;
		}
		
		public ConnectParams<T> build(){
			return new ConnectParams<T>(this);
		}
	}
	
}
