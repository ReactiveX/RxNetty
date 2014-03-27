package io.reactivex.netty;

import java.util.Map;

public class SubscribeInfo {

	private String host;
	private int port;
	private String name;
	private Map<String,String> subscribeParameters;
	
	public SubscribeInfo(String host, int port, String name,
			Map<String, String> subscribeParameters) {
		this.host = host;
		this.port = port;
		this.name = name;
		this.subscribeParameters = subscribeParameters;
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

	public Map<String, String> getSubscribeParameters() {
		return subscribeParameters;
	}
}
