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
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.slotting.SlottingStrategies;
import io.reactivex.netty.slotting.SlottingStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;


public class RemoteObservable {

	private RemoteObservable(){}
	
	public static <T> Observable<T> connect(final String host, final int port, final String name, 
			final Map<String,String> subscribeParameters, final Decoder<T> decoder){
		return Observable.create(new OnSubscribe<T>(){
			@Override
			public void call(Subscriber<? super T> subscriber) {
				RemoteUnsubscribe remoteUnsubscribe = new RemoteUnsubscribe(name);
				// wrapped in Observable.create() to inject unsubscribe callback
				subscriber.add(remoteUnsubscribe); // unsubscribed callback
				createTcpConnectionToServer(host, port, name, subscribeParameters, decoder, remoteUnsubscribe).subscribe(subscriber);
			}
		});
	}
	
	public static <T> Observable<T> connect(final String host, final int port, String name, final Decoder<T> decoder){
		return connect(host, port, name, null, decoder);
	}
	
	public static <T> Observable<T> connect(final String host, final int port, final Decoder<T> decoder){
		return connect(host, port, null, null, decoder);
	}
	
	public static <T> Observable<T> connect(final String host, final int port, final Map<String,String> subscribeParameters, 
			final Decoder<T> decoder){
		return connect(host, port, null, subscribeParameters, decoder);
	}
	
	private static <T> Observable<T> createTcpConnectionToServer(String host, int port, final String name, 
			final Map<String,String> subscribeParameters, final Decoder<T> decoder, final RemoteUnsubscribe remoteUnsubscribe){
		return RxNetty.createTcpClient(host, port, new PipelineConfiguratorComposite<RemoteRxEvent, RemoteRxEvent>(
				new PipelineConfigurator<RemoteRxEvent, RemoteRxEvent>(){
					@Override
					public void configureNewPipeline(ChannelPipeline pipeline) {
//						pipeline.addFirst(new LoggingHandler(LogLevel.ERROR)); // uncomment to enable debug logging				
						pipeline.addLast("frameEncoder", new LengthFieldPrepender(4)); // 4 bytes to encode length
						pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(524288, 0, 4, 0, 4)); // max frame = half MB
					}
				}, new RxEventPipelineConfigurator()))
			.connect()
			.flatMap(new Func1<ObservableConnection<RemoteRxEvent, RemoteRxEvent>, Observable<RemoteRxEvent>>(){
			@Override
			public Observable<RemoteRxEvent> call(final ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
				connection.writeAndFlush(RemoteRxEvent.subscribed(name, subscribeParameters)); // send subscribe event to server
				remoteUnsubscribe.setConnection(connection);
				return connection.getInput();
			}
		})
		// data received form server
		.map(new Func1<RemoteRxEvent,Notification<T>>(){
			@Override
			public Notification<T> call(RemoteRxEvent rxEvent) {
				if (rxEvent.getType() == RemoteRxEvent.Type.next){
					return Notification.createOnNext(decoder.decode(rxEvent.getData()));
				}else if (rxEvent.getType() == RemoteRxEvent.Type.error){
					return Notification.createOnError(fromBytesToThrowable(rxEvent.getData()));
				}else if (rxEvent.getType() == RemoteRxEvent.Type.completed){
					return Notification.createOnCompleted();
				}else{
					throw new RuntimeException("RemoteRxEvent of type:"+rxEvent.getType()+", not supported.");
				}
			}
		})
		.<T>dematerialize();
	}
	public static <T> RemoteObservableServer serve(int port, final Observable<T> observable, final Encoder<T> encoder){
		return serve(configureServerFromParams(null, port, observable, encoder, SlottingStrategies.<T>noSlotting(), 
				IngressPolicies.allowAll()));
	}
	
	public static <T> RemoteObservableServer serve(int port, String name, final Observable<T> observable, final Encoder<T> encoder){
				return serve(configureServerFromParams(name, port, observable, encoder, SlottingStrategies.<T>noSlotting(),
						IngressPolicies.allowAll()));
	}
	
	@SuppressWarnings("rawtypes") // allow many different type configurations to be served
	static RemoteObservableServer serve(RemoteObservableServer.Builder builder){
		int port = builder.getPort();
		// setup configuration state for server
		Map<String,RemoteObservableConfiguration> configLookupByName = new HashMap<String, RemoteObservableConfiguration>();
		for(RemoteObservableConfiguration config : builder.getObservablesConfigured()){
			configLookupByName.put(config.getName(), config);
		}
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
				new RemoteObservableConnectionHandler(configLookupByName, builder.getIngressPolicy()));
		return new RemoteObservableServer(server);
	}
	
	private static <T> RemoteObservableServer.Builder configureServerFromParams(String name, int port, Observable<T> observable, 
			Encoder<T> encoder,	SlottingStrategy<T> slottingStrategy, IngressPolicy ingressPolicy){
		return new RemoteObservableServer
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
