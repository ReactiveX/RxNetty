package io.reactivex.netty;

import io.reactivex.netty.ingress.IngressPolicies;
import io.reactivex.netty.ingress.IngressPolicy;
import io.reactivex.netty.server.RxServer;

import java.util.HashSet;
import java.util.Set;

public class RemoteObservableServer {

	private RxServer<RemoteRxEvent, RemoteRxEvent> server;
	
	RemoteObservableServer(RxServer<RemoteRxEvent, RemoteRxEvent> server) {
		this.server = server;
	}
	
	public void start(){
		server.start();
	}
	
	public void startAndWait(){
		server.startAndWait();
	}
	
	public void shutdown(){
		try {
			server.shutdown();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class Builder{
		
		private int port;
		@SuppressWarnings("rawtypes")
		private Set<RemoteObservableConfiguration> observablesConfigured 
			= new HashSet<RemoteObservableConfiguration>();
		private IngressPolicy ingressPolicy = IngressPolicies.allowAll();
		
		public Builder port(int port){
			this.port = port;
			return this;
		}
		
		public Builder ingressPolicy(IngressPolicy ingressPolicy){
			this.ingressPolicy = ingressPolicy;
			return this;
		}
		
		public <T> Builder addObservable(RemoteObservableConfiguration<T> configuration){
			observablesConfigured.add(configuration);
			return this;
		}
		
		public RemoteObservableServer build(){
			return RemoteObservable.serve(this);
		}

		int getPort() {
			return port;
		}

		@SuppressWarnings("rawtypes")
		Set<RemoteObservableConfiguration> getObservablesConfigured() {
			return observablesConfigured;
		}

		IngressPolicy getIngressPolicy() {
			return ingressPolicy;
		}
	}
}
