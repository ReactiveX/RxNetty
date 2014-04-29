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

import java.util.Map;

import rx.functions.Action2;

import io.reactivex.netty.codec.Decoder;

public class ConnectConfiguration<T> {

	private String host;
	private int port;
	private String name;
	private Decoder<T> decoder;
	private Map<String,String> subscribeParameters;
	private int subscribeRetryAttempts;
	private Action2<SubscribeInfo, Throwable> subscribeErrorHandler;
	private boolean suppressSubscribeErrors = false;
	private Action2<T, Throwable> deocdingErrorHandler;
	private boolean suppressDecodingErrors = false;
	
	ConnectConfiguration(Builder<T> builder){
		this.host = builder.host;
		this.name = builder.name;
		this.port = builder.port;
		this.decoder = builder.decoder;
		this.subscribeParameters = builder.subscribeParameters;
		this.subscribeRetryAttempts = builder.subscribeRetryAttempts;
		this.subscribeErrorHandler = builder.subscribeErrorHandler;
		this.suppressSubscribeErrors = builder.suppressSubscribeErrors;
		this.deocdingErrorHandler = builder.deocdingErrorHandler;
		this.suppressDecodingErrors = builder.suppressDecodingErrors;
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

	public int getSubscribeRetryAttempts() {
		return subscribeRetryAttempts;
	}

	public Action2<SubscribeInfo, Throwable> getSubscribeErrorHandler() {
		return subscribeErrorHandler;
	}
	
	public boolean isSuppressSubscribeErrors() {
		return suppressSubscribeErrors;
	}

	public Action2<T, Throwable> getDeocdingErrorHandler() {
		return deocdingErrorHandler;
	}
	
	public boolean isSuppressDecodingErrors() {
		return suppressDecodingErrors;
	}
	
	public static class Builder<T>{
		
		private String host;
		private int port;
		private String name;
		private Decoder<T> decoder;
		private Map<String,String> subscribeParameters;
		private int subscribeRetryAttempts = 3;
		private Action2<SubscribeInfo, Throwable> subscribeErrorHandler = new Action2<SubscribeInfo, Throwable>(){
			@Override
			public void call(SubscribeInfo t1, Throwable t2) {
				t2.printStackTrace();
			}
		}; 
		private boolean suppressSubscribeErrors = false;
		private Action2<T, Throwable> deocdingErrorHandler = new Action2<T, Throwable>(){
			@Override
			public void call(T t1, Throwable t2) {
				t2.printStackTrace();
			}
		};
		private boolean suppressDecodingErrors = false;
		
		public Builder<T> host(String host){
			this.host = host;
			return this;
		}
		
		public Builder<T> port(int port){
			this.port = port;
			return this;
		}
		
		public Builder<T> subscribeErrorHandler(Action2<SubscribeInfo, Throwable> handler, boolean suppressSubscribeErrors){
			this.subscribeErrorHandler = handler;
			this.suppressSubscribeErrors = suppressSubscribeErrors;
			return this;
		}
		
		public Builder<T> deocdingErrorHandler(Action2<T, Throwable> handler, boolean suppressDecodingErrors){
			this.deocdingErrorHandler = handler;
			this.suppressDecodingErrors = suppressDecodingErrors;
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
		
		public Builder<T> subscribeRetryAttempts(int subscribeRetryAttempts){
			this.subscribeRetryAttempts = subscribeRetryAttempts;
			return this;
		}
		
		public ConnectConfiguration<T> build(){
			return new ConnectConfiguration<T>(this);
		}
	}
	
}
