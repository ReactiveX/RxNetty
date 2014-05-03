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

import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.codec.Encoder;
import io.reactivex.netty.ingress.IngressPolicy;
import io.reactivex.netty.slotting.SlotAssignment;
import io.reactivex.netty.slotting.SlotValuePair;
import io.reactivex.netty.slotting.SlottingStrategy;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;

public class RemoteObservableConnectionHandler implements
		ConnectionHandler<RemoteRxEvent, RemoteRxEvent> {
	
    private static final Logger logger = LoggerFactory.getLogger(RemoteObservableConnectionHandler.class);
	
	@SuppressWarnings("rawtypes")
	private Map<String,RemoteObservableConfiguration> observables;
	private CountDownLatch blockUntilCompleted;
	private ServerMetrics serverMetrics; 
	private IngressPolicy ingressPolicy;

	@SuppressWarnings("rawtypes")
	public RemoteObservableConnectionHandler(
			Map<String,RemoteObservableConfiguration> observables,
			IngressPolicy ingressPolicy, CountDownLatch blockUntilCompleted,
			ServerMetrics metrics) {
		this.observables = observables;
		this.ingressPolicy = ingressPolicy;
		this.blockUntilCompleted = blockUntilCompleted;
		this.serverMetrics = metrics;
	}

	@Override
	public Observable<Void> handle(final
			ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
		logger.debug("Connection received: "+connection.toString());
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
							connection.writeAndFlush(RemoteRxEvent.completed(event.getName()));
							blockUntilCompleted.countDown();
							serverMetrics.incrementCompletedCount();
						}
						@Override
						public void onError(
								Throwable e) {
							connection.writeAndFlush(RemoteRxEvent.error(event.getName(), RemoteObservable.fromThrowableToBytes(e)));
							serverMetrics.incrementErrorCount();
						}
						@Override
						public void onNext(T t) {
							connection.writeAndFlush(RemoteRxEvent.next(event.getName(), encoder.encode(t)));
							serverMetrics.incrementNextCount();
						}
			});
			unsubscribeCallbackReference.setValue(subscription);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Observable<Void> handleSubscribeRequest(RemoteRxEvent event, final ObservableConnection<RemoteRxEvent, 
			RemoteRxEvent> connection, MutableReference<SlottingStrategy> slottingStrategyReference, 
			MutableReference<Subscription> unsubscribeCallbackReference) {
		
		// check if observable exists in configs
		String observableName = event.getName();
		RemoteObservableConfiguration config = observables.get(observableName);
		if (config == null){
			return Observable.error(new RemoteObservableException("No remote observable configuration found for name: "+observableName));
		}

		if (event.getType() == RemoteRxEvent.Type.subscribed){
			SlottingStrategy slottingStrategy = config.getSlottingStrategy();
			slottingStrategyReference.setValue(slottingStrategy);
			SlotAssignment slotAssignment = slottingStrategy.assignSlot(connection);
			if (slotAssignment.isAssigned()){
				subscribe(unsubscribeCallbackReference, event, connection, config, slotAssignment);
				serverMetrics.incrementSubscribedCount();
				logger.debug("Connection: "+connection.toString()+" subscribed to observable: "+observableName);
			}else{
				return Observable.error(new RemoteObservableException("Slot could not be assigned for connection."));
			}
		}
		return Observable.empty();
	}

	@SuppressWarnings("rawtypes")
	private Observable<Void> setupConnection(final ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
		
		// state associated with connection
		// used to initiate 'unsubscribe' callback to subscriber
		final MutableReference<Subscription> unsubscribeCallbackReference = new MutableReference<Subscription>();
		// used to release slot when connection completes
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
				.flatMap(new Func1<RemoteRxEvent, Observable<Void>>(){
					@Override
					public Observable<Void> call(RemoteRxEvent event) {
						if (event.getType() == RemoteRxEvent.Type.subscribed){
							return handleSubscribeRequest(event, connection, slottingStrategyReference, unsubscribeCallbackReference);
						}else if (event.getType() == RemoteRxEvent.Type.unsubscribed){
							Subscription subscription = unsubscribeCallbackReference.getValue();
							if (subscription != null){
								subscription.unsubscribe();
							}
							releaseSlot(slottingStrategyReference.getValue(), connection);
							serverMetrics.incrementUnsubscribedCount();
							logger.debug("Connection: "+connection.toString()+" unsubscribed");
						} 
						return Observable.empty();
					}
				})
				.doOnCompleted(new Action0(){
					@Override
					public void call() {
						// connection closed, remote slot
						releaseSlot(slottingStrategyReference.getValue(), connection);
						logger.debug("Connection: "+connection.toString()+" closed");
					}
				});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void releaseSlot(SlottingStrategy slottingStrategy, ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection){
		if (slottingStrategy != null){
			slottingStrategy.releaseSlot(connection);
		}
	}
	
}
