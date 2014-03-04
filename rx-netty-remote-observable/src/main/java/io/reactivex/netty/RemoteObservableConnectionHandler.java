package io.reactivex.netty;

import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.codec.Encoder;
import io.reactivex.netty.ingress.IngressPolicy;
import io.reactivex.netty.slotting.SlotAssignment;
import io.reactivex.netty.slotting.SlotValuePair;
import io.reactivex.netty.slotting.SlottingStrategy;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class RemoteObservableConnectionHandler<T> implements
		ConnectionHandler<RemoteRxEvent, RemoteRxEvent> {
		
	private SlottingStrategy<T> slottingStrategy;	
	private Observable<List<Observable<T>>> observable;
	private Encoder<T> encoder;
	private Func1<SlotValuePair<T>,Boolean> slotAssignmentFunction;
	private IngressPolicy ingressPolicy;

	public RemoteObservableConnectionHandler(
			Observable<List<Observable<T>>> observable,
			Encoder<T> encoder, SlottingStrategy<T> slottingStrategy, 
			IngressPolicy ingressPolicy) {
		this.observable = observable;
		this.encoder = encoder;
		this.slottingStrategy = slottingStrategy;
		this.slotAssignmentFunction = slottingStrategy.slottingFunction();
		this.ingressPolicy = ingressPolicy;
	}

	@Override
	public Observable<Void> handle(final
			ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
		
		// used to initiate 'unsubscribe' callback to subscriber
		MutableReference<Subscription> unsubscribeCallbackReference = new MutableReference<Subscription>();
		// used to signal a new list of observables is available for processing
		MutableReference<Subscription> mergedObservableListReference = new MutableReference<Subscription>();

		if (ingressPolicy.allowed(connection)){
			SlotAssignment slotAssignment = slottingStrategy.assignSlot(connection);
			if (slotAssignment.isAssigned()){
				return setupConnection(connection, slotAssignment, unsubscribeCallbackReference, mergedObservableListReference);
			}else{
				Exception e = new RemoteObservableException("Slot could not be assigned for connection.");
				connection.writeAndFlush(RemoteRxEvent.error(RemoteObservable.fromThrowableToBytes(e)));
				return Observable.error(e); // TODO disconnect stale connection
			}
		}else{
			Exception e = new RemoteObservableException("Connection rejected due to ingress policy");
			connection.writeAndFlush(RemoteRxEvent.error(RemoteObservable.fromThrowableToBytes(e)));
			return Observable.error(e);
		}
	}

	private Observable<Void> setupConnection(
			final ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection, 
			final SlotAssignment slotAssignment, final MutableReference<Subscription> unsubscribeCallbackReference, 
			final MutableReference<Subscription> mergedObservableListReference) {
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
				// handle request
				}).map(new Func1<RemoteRxEvent,Void>(){
					@Override
					public Void call(RemoteRxEvent event) {
						if (event.getType() == RemoteRxEvent.Type.subscribed){
							// write RxEvents over the connection
							Subscription subscribedSubscription = observable
								.subscribe(new Action1<List<Observable<T>>>(){
									@Override
									public void call(
											List<Observable<T>> list) {
										// check if connection has set of merged subscriptions
										Subscription mergedSubscriptionCheck = mergedObservableListReference.getValue();
										if (mergedSubscriptionCheck != null){
											// initiate unsubscribe callback to 
											mergedSubscriptionCheck.unsubscribe();
										}
										if (list != null && !list.isEmpty()){													
											Subscription mergedSubscription = Observable
													.merge(list)
													.map(new Func1<T, SlotValuePair<T>>(){
														@Override
														public SlotValuePair<T> call(
																T t1) {
															return new SlotValuePair<T>(slotAssignment.getSlotAssignment(), t1);
														}
													})
													.filter(slotAssignmentFunction)
													.map(new Func1<SlotValuePair<T>,T>(){
														@Override
														public T call(
																SlotValuePair<T> pair) {
															return pair.getValue();
														}
													})
													.subscribe(new Observer<T>(){
														@Override
														public void onCompleted() {
															connection.writeAndFlush(RemoteRxEvent.completed());
														}
														@Override
														public void onError(
																Throwable e) {
															connection.writeAndFlush(RemoteRxEvent.error(RemoteObservable.fromThrowableToBytes(e)));
														}
														@Override
														public void onNext(T t) {															
															connection.writeAndFlush(RemoteRxEvent.next(encoder.encode(t)));
														}
											});
											mergedObservableListReference.setValue(mergedSubscription);
										}
									}
								});
								unsubscribeCallbackReference.setValue(subscribedSubscription);
						}else if (event.getType() == RemoteRxEvent.Type.unsubscribed){
							Subscription subscription = unsubscribeCallbackReference.getValue();
							if (subscription != null){
								unsubscribeCallbackReference.getValue().unsubscribe();
							}
						}
						return null;
					}
				}).doOnCompleted(new Action0(){
					@Override
					public void call() {
						// connection closed, remote slot
						slottingStrategy.releaseSlot(connection);
					}
				});
	}
	
	

}
