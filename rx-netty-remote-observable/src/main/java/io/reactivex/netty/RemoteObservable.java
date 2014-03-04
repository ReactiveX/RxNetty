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
import java.util.ArrayList;
import java.util.List;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;


public class RemoteObservable {

	private RemoteObservable(){}
	
	public static <T> Observable<T> connect(final String host, final int port, final Decoder<T> decoder){
		return Observable.create(new OnSubscribe<T>(){
			@Override
			public void call(Subscriber<? super T> subscriber) {
				RemoteUnsubscribe remoteUnsubscribe = new RemoteUnsubscribe();
				// wrapped in Observable.create() to inject unsubscribe callback
				subscriber.add(remoteUnsubscribe); // unsubscribed callback
				createTcpConnectionToServer(host, port, decoder, remoteUnsubscribe).subscribe(subscriber);
			}
		});
	}
	
	private static <T> Observable<T> createTcpConnectionToServer(String host, int port, final Decoder<T> decoder, 
			final RemoteUnsubscribe remoteUnsubscribe){
		return RxNetty.createTcpClient(host, port, new PipelineConfiguratorComposite<RemoteRxEvent, RemoteRxEvent>(
				new PipelineConfigurator<RemoteRxEvent, RemoteRxEvent>(){
					@Override
					public void configureNewPipeline(ChannelPipeline pipeline) {
//						pipeline.addFirst(new LoggingHandler(LogLevel.ERROR)); // uncomment to enable debug logging				
						pipeline.addLast("frameEncoder", new LengthFieldPrepender(4)); // 4 bytes to encode length
						pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(524288, 0, 4, 0, 4)); // max frame = half MB
					}
				}, new RxEventPipelineConfigurator()))
			.connect().flatMap(new Func1<ObservableConnection<RemoteRxEvent, RemoteRxEvent>, Observable<RemoteRxEvent>>(){
			@Override
			public Observable<RemoteRxEvent> call(final ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
				connection.writeAndFlush(RemoteRxEvent.subscribed()); // send subscribe event to server
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
	
	public static <T> void serve(int port, final Observable<T> observable, final Encoder<T> encoder){
		serveMany(port, toObservableListOfOne(observable), encoder, false, SlottingStrategies.<T>noSlotting(), 
				IngressPolicies.allowAll());
	}
	
	public static <T> void serve(int port, final Observable<T> observable, final Encoder<T> encoder, 
			SlottingStrategy<T> slottingStrategy){
		serveMany(port, toObservableListOfOne(observable), encoder, false, slottingStrategy, IngressPolicies.allowAll());
	}
	
	public static <T> void serveAndWait(int port, final Observable<T> observable, final Encoder<T> encoder){
		serveMany(port, toObservableListOfOne(observable), encoder, true, SlottingStrategies.<T>noSlotting(),
				IngressPolicies.allowAll());
	}
	
	public static <T> void serveAndWait(int port, final Observable<T> observable, final Encoder<T> encoder,
			SlottingStrategy<T> slottingStrategy, IngressPolicy ingressPolicy){
		serveMany(port, toObservableListOfOne(observable), encoder, true, slottingStrategy,
				ingressPolicy);
	}
	
	public static <T> void serveMany(int port, final Observable<List<Observable<T>>> observable, final Encoder<T> encoder,
			SlottingStrategy<T> slottingStrategy, IngressPolicy ingressPolicy){
		serveMany(port, observable, encoder, false, slottingStrategy, ingressPolicy);
	}
	
	public static <T> void serveMany(int port, final Observable<List<Observable<T>>> observable, final Encoder<T> encoder){
		serveMany(port, observable, encoder, false, SlottingStrategies.<T>noSlotting(), IngressPolicies.allowAll());
	}
	
	public static <T> void serveManyAndWait(int port, final Observable<List<Observable<T>>> observable, final Encoder<T> encoder,
			SlottingStrategy<T> slottingStrategy, IngressPolicy ingressPolicy){
		serveMany(port, observable, encoder, true, slottingStrategy, ingressPolicy);
	}
	
	public static <T> void serveManyAndWait(int port, final Observable<List<Observable<T>>> observable, final Encoder<T> encoder){
		serveMany(port, observable, encoder, true, SlottingStrategies.<T>noSlotting(), IngressPolicies.allowAll());
	}
	
	private static <T> Observable<List<Observable<T>>> toObservableListOfOne(Observable<T> observable){
		List<Observable<T>> list = new ArrayList<Observable<T>>(1);
		list.add(observable);
		ReplaySubject<List<Observable<T>>> subject = ReplaySubject.create(1);
		subject.onNext(list);
		return subject;
	}
	
	private static <T> void serveMany(int port, final Observable<List<Observable<T>>> observable, final Encoder<T> encoder,
			boolean startAndWait, SlottingStrategy<T> slottingStrategy, IngressPolicy ingressPolicy){
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
				new RemoteObservableConnectionHandler<T>(observable, encoder, slottingStrategy, ingressPolicy));
		if(startAndWait){
			server.startAndWait();
		}else{
			server.start();
		}
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
