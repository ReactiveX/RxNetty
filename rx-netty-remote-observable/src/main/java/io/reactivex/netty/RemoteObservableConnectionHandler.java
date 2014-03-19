package io.reactivex.netty;

import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.codec.Encoder;
import io.reactivex.netty.ingress.IngressPolicy;
import io.reactivex.netty.slotting.SlotAssignment;
import io.reactivex.netty.slotting.SlotValuePair;
import io.reactivex.netty.slotting.SlottingStrategy;

import java.util.Map;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;

public class RemoteObservableConnectionHandler implements
		ConnectionHandler<RemoteRxEvent, RemoteRxEvent> {
	
	@SuppressWarnings("rawtypes")
	private Map<String,RemoteObservableConfiguration> observables;
	private IngressPolicy ingressPolicy;

	@SuppressWarnings("rawtypes")
	public RemoteObservableConnectionHandler(
			Map<String,RemoteObservableConfiguration> observables,
			IngressPolicy ingressPolicy) {
		this.observables = observables;
		this.ingressPolicy = ingressPolicy;
	}

	@Override
	public Observable<Void> handle(final
			ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {		
		if (ingressPolicy.allowed(connection)){
				return setupConnection(connection);
		}else{
			Exception e = new RemoteObservableException("Connection rejected due to ingress policy");
			return Observable.error(e);
		}
	}
	
	private <T> void subscribe(final MutableReference<Subscription> unsubscribeCallbackReference, 
			final RemoteRxEvent event, final ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection, 
			RemoteObservableConfiguration<T> configuration, final SlotAssignment slotAssignment){
	
			final Observable<T> observable = configuration.getObservable();
			final Func1<Map<String,String>, Func1<T,Boolean>> filterFunction = configuration.getFilterFunction();
			SlottingStrategy<T> slottingStrategy = configuration.getSlottingStrategy();
			final Func1<SlotValuePair<T>,Boolean> slotAssignmentFunction = slottingStrategy.slottingFunction();
			final Encoder<T> encoder = configuration.getEncoder();
			
			// write RxEvents over the connection		
			Subscription subscription = observable.
					filter(filterFunction.call(event.getSubscribeParameters()))
					// setup slotting
					.map(new Func1<T, SlotValuePair<T>>(){
						@Override
						public SlotValuePair<T> call(
								T t1) {
							return new SlotValuePair<T>(slotAssignment.getSlotAssignment(), t1);
						}
					})
					// apply slotting function
					.filter(slotAssignmentFunction)
					// get value for processing
					.map(new Func1<SlotValuePair<T>,T>(){
						@Override
						public T call(
								SlotValuePair<T> pair) {
							return pair.getValue();
						}
					})
					// send results out
					.subscribe(new Observer<T>(){
						@Override
						public void onCompleted() {
							connection.writeAndFlush(RemoteRxEvent.completed(event.getObservableName()));
						}
						@Override
						public void onError(
								Throwable e) {
							connection.writeAndFlush(RemoteRxEvent.error(event.getObservableName(), RemoteObservable.fromThrowableToBytes(e)));
						}
						@Override
						public void onNext(T t) {
							connection.writeAndFlush(RemoteRxEvent.next(event.getObservableName(), encoder.encode(t)));
						}
			});
			unsubscribeCallbackReference.setValue(subscription);
	}
	
	private void unsubscribe(MutableReference<Subscription> unsubscribeCallbackReference){
		Subscription subscription = unsubscribeCallbackReference.getValue();
		if (subscription != null){
			// unsubscribe to list of observables
			unsubscribeCallbackReference.getValue().unsubscribe();
		}
	}

	@SuppressWarnings("rawtypes")
	private Observable<Void> setupConnection(final ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
		
		// used to initiate 'unsubscribe' callback to subscriber
		final MutableReference<Subscription> unsubscribeCallbackReference = new MutableReference<Subscription>();
		final MutableReference<SlottingStrategy> slottingStrategyReference = new MutableReference<SlottingStrategy>();
		
		return connection.getInput()
				// filter out unsupported operations
				.filter(new Func1<RemoteRxEvent,Boolean>(){
					@Override
					public Boolean call(RemoteRxEvent event) {
						boolean supportedOperation = false;
						if (event.getType() == RemoteRxEvent.Type.subscribed || 
								event.getType() == RemoteRxEvent.Type.unsubscribed){
							supportedOperation = true;
						}
						return supportedOperation;
					}
				})
				// handle request
				.flatMap(new Func1<RemoteRxEvent,Observable<Void>>(){
					@SuppressWarnings({ "unchecked" })
					@Override
					public Observable<Void> call(final RemoteRxEvent event) {
						// check if observable exists
						String observableName = event.getObservableName();
						RemoteObservableConfiguration config = observables.get(observableName);
						if (config == null){
							return Observable.error(new RemoteObservableException("No remote observable configuration found for name: "+observableName));
						}
						if (event.getType() == RemoteRxEvent.Type.subscribed){
							SlottingStrategy slottingStrategy = config.getSlottingStrategy();
							slottingStrategyReference.setValue(slottingStrategy);
							SlotAssignment slotAssignment = slottingStrategy.assignSlot(connection);
							if (slotAssignment.isAssigned()){
								subscribe(unsubscribeCallbackReference, event, 
										connection,	config, slotAssignment);
							}else{
								return Observable.error(new RemoteObservableException("Slot could not be assigned for connection."));
							}
						}else if (event.getType() == RemoteRxEvent.Type.unsubscribed){
							unsubscribe(unsubscribeCallbackReference);
						}
						return Observable.empty();
					}
				})
				.doOnCompleted(new Action0(){
					@SuppressWarnings("unchecked")
					@Override
					public void call() {
						// connection closed, remote slot
						SlottingStrategy slottingStrategy = slottingStrategyReference.getValue();
						if (slottingStrategy != null){
							slottingStrategy.releaseSlot(connection);
						}
					}
				});
	}
}
