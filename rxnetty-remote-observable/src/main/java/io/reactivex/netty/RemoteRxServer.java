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

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.reactivex.netty.ingress.IngressPolicies;
import io.reactivex.netty.ingress.IngressPolicy;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.server.RxServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteRxServer {

    private static final Logger logger = LoggerFactory.getLogger(RemoteRxServer.class);

	private RxServer<RemoteRxEvent, RemoteRxEvent> server;
	private CountDownLatch blockUntilCompleted;
	private ServerMetrics metrics;
	private int port;
	
	RemoteRxServer(RxServer<RemoteRxEvent, RemoteRxEvent> server, ServerMetrics metrics) {
		this.server = server;
		this.metrics = metrics;
	}
	
	RemoteRxServer(RxServer<RemoteRxEvent, RemoteRxEvent> server, CountDownLatch blockUntilCompleted,
			ServerMetrics metrics) {
		this(server, metrics);
		this.blockUntilCompleted = blockUntilCompleted;
	}

	public ServerMetrics getMetrics() {
		return metrics;
	}

	public void start(){
		server.start();
	}
	
	public void startAndWait(){
		server.startAndWait();
		logger.info("RemoteRxServer shutdown on port: "+port);
	}
	
	public void shutdown(){
		try {
			server.shutdown();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		logger.info("RemoteRxServer shutdown on port: "+port);
	}
	
	public void blockUntilCompleted(){
		try {
			blockUntilCompleted.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // TODO
		}
		logger.info("RemoteRxServer shutdown on port: "+port);
	}
	
	@SuppressWarnings("rawtypes")
	public RemoteRxServer(Builder builder){
		port = builder.getPort();
		// setup configuration state for server
		Map<String,RemoteObservableConfiguration> configuredObservables = new HashMap<String, RemoteObservableConfiguration>();
		// add configs
		for(RemoteObservableConfiguration config : builder.getObservablesConfigured()){
			String observableName = config.getName();
			logger.debug("RemoteRxServer configured with remote observable: "+observableName);
			configuredObservables.put(observableName, config);
		}
		metrics = new ServerMetrics();
		blockUntilCompleted = new CountDownLatch(1);
		// create server
		RxServer<RemoteRxEvent, RemoteRxEvent> server 
			= RxNetty.createTcpServer(port, new PipelineConfiguratorComposite<RemoteRxEvent, RemoteRxEvent>(
				new PipelineConfigurator<RemoteRxEvent, RemoteRxEvent>(){
					@Override
					public void configureNewPipeline(ChannelPipeline pipeline) {
//						pipeline.addFirst(new LoggingHandler(LogLevel.ERROR)); // uncomment to enable debug logging	
						pipeline.addLast("frameEncoder", new LengthFieldPrepender(4)); // 4 bytes to encode length
						pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(524288, 0, 4, 0, 4)); // max frame = half MB
					}
				}, new RxEventPipelineConfigurator()), 	
				new RemoteObservableConnectionHandler(configuredObservables, builder.getIngressPolicy(), blockUntilCompleted,
						metrics));
		this.server = server;
		logger.info("RemoteRxServer started on port: "+port);
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
		
		public RemoteRxServer build(){
			return new RemoteRxServer(this);
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
