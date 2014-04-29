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
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.codec.Decoder;
import io.reactivex.netty.codec.Encoder;
import io.reactivex.netty.ingress.IngressPolicies;
import io.reactivex.netty.ingress.IngressPolicy;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.slotting.SlottingStrategies;
import io.reactivex.netty.slotting.SlottingStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;


public class RemoteObservable {
		
	private RemoteObservable(){}
	
	public static <T> RemoteRxConnection<T> connect(final ConnectConfiguration<T> params){
		final ConnectionMetrics metrics = new ConnectionMetrics();
		return new RemoteRxConnection<T>(Observable.create(new OnSubscribe<T>(){
			@Override
			public void call(Subscriber<? super T> subscriber) {
				RemoteUnsubscribe remoteUnsubscribe = new RemoteUnsubscribe(params.getName());
				// wrapped in Observable.create() to inject unsubscribe callback
				subscriber.add(remoteUnsubscribe); // unsubscribed callback
				// create connection
				createTcpConnectionToServer(params, remoteUnsubscribe, metrics)
					.subscribe(subscriber);
			}
		}), metrics);
	}
	
	public static <T> Observable<T> connect(final String host, final int port, final Decoder<T> decoder){
		return connect(new ConnectConfiguration.Builder<T>()
				.host(host)
				.port(port)
				.decoder(decoder)			
				.build()).getObservable();
	}
	
	private static <T> Observable<T> createTcpConnectionToServer(final ConnectConfiguration<T> params, 
			final RemoteUnsubscribe remoteUnsubscribe, final ConnectionMetrics metrics){
		// XXX remove this after onErrorFlatMap Observable.error() + dematerialize is fixed
		final PublishSubject<T> proxy = PublishSubject.create(); // necessary to inject connection errors into observable returned
		// XXX
		final Decoder<T> decoder = params.getDecoder();
		RxNetty.createTcpClient(params.getHost(), params.getPort(), new PipelineConfiguratorComposite<RemoteRxEvent, RemoteRxEvent>(
				new PipelineConfigurator<RemoteRxEvent, RemoteRxEvent>(){
					@Override
					public void configureNewPipeline(ChannelPipeline pipeline) {
//						pipeline.addFirst(new LoggingHandler(LogLevel.ERROR)); // uncomment to enable debug logging				
						pipeline.addLast("frameEncoder", new LengthFieldPrepender(4)); // 4 bytes to encode length
						pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(524288, 0, 4, 0, 4)); // max frame = half MB
					}
				}, new RxEventPipelineConfigurator()))
			.connect()
			// send subscription request, get input stream
			.flatMap(new Func1<ObservableConnection<RemoteRxEvent, RemoteRxEvent>, Observable<RemoteRxEvent>>(){
			@Override
			public Observable<RemoteRxEvent> call(final ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
				connection.writeAndFlush(RemoteRxEvent.subscribed(params.getName(), params.getSubscribeParameters())); // send subscribe event to server
				remoteUnsubscribe.setConnection(connection);
				return connection.getInput();
				}
			})
			// retry subscription attempts
			.retry(params.getSubscribeRetryAttempts())
			// handle subscription errors TODO add back after dematerialize fix
//			.onErrorFlatMap(new Func1<OnErrorThrowable,Observable<RemoteRxEvent>>(){
//				@Override
//				public Observable<RemoteRxEvent> call(OnErrorThrowable t1) {
//					params.getSubscribeErrorHandler().call(params, t1);
//					if (!params.isSuppressSubscribeErrors()){
//						return Observable.error(t1.);
//					}
//					return Observable.empty();
//				}
//			}) 
			// XXX remove this after onErrorFlatMap Observable.error() + dematerialize is fixed
			.doOnError(new Action1<Throwable>(){
				@Override
				public void call(Throwable t1) {
					params.getSubscribeErrorHandler().call(new SubscribeInfo(params.getHost(), params.getPort(),
							params.getName(), params.getSubscribeParameters()), t1);
					if (!params.isSuppressSubscribeErrors()){
						proxy.onError(t1); // inject error into stream
					}
				}
			})
			// XXX
			// data received from server
			.map(new Func1<RemoteRxEvent,Notification<T>>(){
				@Override
				public Notification<T> call(RemoteRxEvent rxEvent) {
					if (rxEvent.getType() == RemoteRxEvent.Type.next){
						metrics.incrementNextCount();
						return Notification.createOnNext(decoder.decode(rxEvent.getData()));
					}else if (rxEvent.getType() == RemoteRxEvent.Type.error){
						metrics.incrementErrorCount();
						return Notification.createOnError(fromBytesToThrowable(rxEvent.getData()));
					}else if (rxEvent.getType() == RemoteRxEvent.Type.completed){
						metrics.incrementCompletedCount();
						return Notification.createOnCompleted();
					}else{
						throw new RuntimeException("RemoteRxEvent of type:"+rxEvent.getType()+", not supported.");
					}
				}
			})
			// handle decoding exceptions
			// XXX TODO replace with onErrorFlatMap after dematerialize fix
			.doOnError(new Action1<Throwable>(){
				@Override
				public void call(Throwable t1) {
					// TODO currently does not support passing value,
					// without onErrorFlatMap fix, settle for null
					params.getDeocdingErrorHandler().call(null, t1);
					if (!params.isSuppressDecodingErrors()){
						proxy.onError(t1);
					}
				}
			})
			// XXX 
			.<T>dematerialize()
			// XXX remove this after onErrorFlatMap Observable.error() + dematerialize is fixed
			.subscribe(new Observer<T>(){
				@Override
				public void onCompleted() {
					proxy.onCompleted();
				}
				@Override
				public void onError(Throwable e) {
					proxy.onError(e);
				}
				@Override
				public void onNext(T t) {
					proxy.onNext(t);
				}
			});
		return proxy;
		// XXX
	}
	public static <T> RemoteRxServer serve(int port, final Observable<T> observable, final Encoder<T> encoder){
		return new RemoteRxServer(configureServerFromParams(null, port, observable, encoder, SlottingStrategies.<T>noSlotting(), 
				IngressPolicies.allowAll()));
	}
	
	public static <T> RemoteRxServer serve(int port, String name, final Observable<T> observable, final Encoder<T> encoder){
				return new RemoteRxServer(configureServerFromParams(name, port, observable, encoder, SlottingStrategies.<T>noSlotting(),
						IngressPolicies.allowAll()));
	}
	
	private static <T> RemoteRxServer.Builder configureServerFromParams(String name, int port, Observable<T> observable, 
			Encoder<T> encoder,	SlottingStrategy<T> slottingStrategy, IngressPolicy ingressPolicy){
		return new RemoteRxServer
				.Builder()
				.port(port)
				.ingressPolicy(ingressPolicy)
				.addObservable(new RemoteObservableConfiguration.Builder<T>()
						.name(name)
						.encoder(encoder)
						.slottingStrategy(slottingStrategy)
						.observable(observable)
						.build());
	}
		
	static byte[] fromThrowableToBytes(Throwable t){
		ByteArrayOutputStream baos = null;
		ObjectOutput out = null;
		try{
			baos = new ByteArrayOutputStream();
			out = new ObjectOutputStream(baos);   
			out.writeObject(t);
		}catch(IOException e){
			throw new RuntimeException(e);
		}finally{
			try{
				if (out != null){out.close();}
				if (baos != null){baos.close();}
			}catch(IOException e1){
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
		}
		return baos.toByteArray();
	}
	
	static Throwable fromBytesToThrowable(byte[] bytes){
		Throwable t = null;
		ByteArrayInputStream bis = null;
		ObjectInput in = null;
		try{
			bis = new ByteArrayInputStream(bytes);
			in = new ObjectInputStream(bis);
			t = (Throwable) in.readObject();
			
		}catch(IOException e){
			throw new RuntimeException(e);
		}catch(ClassNotFoundException e1){
			throw new RuntimeException(e1);
		}finally{
			try {
				if (bis != null){bis.close();}
				if (in != null){in.close();}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return t;
	}
}
