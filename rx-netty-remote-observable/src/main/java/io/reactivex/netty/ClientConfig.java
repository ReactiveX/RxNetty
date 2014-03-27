package io.reactivex.netty;

public abstract class ClientConfig {

	private String host;
	private int port;
	private String name;
	
	public ClientConfig(String host, int port, String name) {
		this.host = host;
		this.port = port;
		this.name = name;
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
}
