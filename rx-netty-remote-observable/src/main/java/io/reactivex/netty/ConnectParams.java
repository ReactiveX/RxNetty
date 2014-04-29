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
