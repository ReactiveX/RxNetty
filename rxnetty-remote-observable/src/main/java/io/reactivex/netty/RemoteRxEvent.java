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


public class RemoteRxEvent {

	private String name;
	public enum Type {next,completed,error,subscribed,unsubscribed}
	private Type type;
	private byte[] data;
	private Map<String,String> subscriptionParameters;
	
	public RemoteRxEvent(String name,
			Type type, byte[] data, Map<String,String> subscriptionParameters) {
		this.name = name;
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
	public String getName() {
		return name;
	}
	Map<String,String> getSubscribeParameters() {
		return subscriptionParameters;
	}
	public static RemoteRxEvent next(String name, byte[] nextData){
		return new RemoteRxEvent(name, Type.next, nextData, null);
	}
	public static RemoteRxEvent completed(String name){
		return new RemoteRxEvent(name, Type.completed, null, null);
	}
	public static RemoteRxEvent error(String name, byte[] errorData){
		return new RemoteRxEvent(name, Type.error, errorData, null);
	}
	public static RemoteRxEvent subscribed(String name, Map<String,String> subscriptionParameters){
		return new RemoteRxEvent(name, Type.subscribed, null, subscriptionParameters);
	}
	public static RemoteRxEvent unsubscribed(String name){
		return new RemoteRxEvent(name, Type.unsubscribed, null, null);
	}
}
