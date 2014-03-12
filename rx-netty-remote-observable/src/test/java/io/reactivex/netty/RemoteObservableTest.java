package io.reactivex.netty;

import io.reactivex.netty.codec.Codecs;
import io.reactivex.netty.filter.ServerSideFilters;
import io.reactivex.netty.slotting.SlottingStrategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subjects.ReplaySubject;

public class RemoteObservableTest {

	@Test
	public void testServeObservable() throws InterruptedException{
		// setup
		Observable<Integer> os = Observable.range(0, 101);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		RemoteObservableServer server = RemoteObservable.serve(serverPort, os, Codecs.integer());
		server.start();
		// connect
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		// assert
		System.out.println("waiting for results");
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-100
			}
		});		
	}
	
	@Test
	public void testServeObservableByName() throws InterruptedException{
		// setup
		Observable<Integer> os = Observable.range(0, 101);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		RemoteObservableServer server = RemoteObservable.serve(serverPort, "integers-from-0-100", os, Codecs.integer());
		server.start();
		// connect to observable by name
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, "integers-from-0-100", Codecs.integer());
		// assert
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-100
			}
		});		
	}
	
	@Test
	public void testServeTwoObservablesOnSamePort() throws InterruptedException{
		// setup
		Observable<Integer> os1 = Observable.range(0, 101);
		Observable<String> os2 = Observable.from(new String[]{"a","b","c","d","e"});
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();

		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(serverPort)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.name("ints")
					.encoder(Codecs.integer())
					.observable(os1)
					.build())
			.addObservable(new RemoteObservableConfiguration.Builder<String>()
					.name("strings")
					.encoder(Codecs.string())
					.observable(os2)
					.build())
			.build();
		
		server.start();
		// connect to observable by name
		Observable<Integer> ro1 = RemoteObservable.connect("localhost", serverPort, "ints", Codecs.integer());
		Observable<String> ro2 = RemoteObservable.connect("localhost", serverPort, "strings", Codecs.string());

		// assert
		Observable.sumInteger(ro1).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-100
			}
		});	
		ro2.reduce(new Func2<String,String,String>(){
			@Override
			public String call(String t1, String t2) {
				return t1+t2; // concat string
			}
		}).toBlockingObservable().forEach(new Action1<String>(){
			@Override
			public void call(String t1) {
				Assert.assertEquals("abcde", t1);
			}
		});
	}
	
	@Test
	public void testServeListObservables(){
		// setup
		Observable<Integer> os1 = Observable.range(0, 101);
		Observable<Integer> os2 = Observable.range(100, 101);
		List<Observable<Integer>> toServe = new LinkedList<Observable<Integer>>();
		toServe.add(os1);
		toServe.add(os2);
		ReplaySubject <List<Observable<Integer>>> subject = ReplaySubject.create();
		subject.onNext(toServe);
		
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int port = portSelector.acquirePort();
		
		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(port)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.encoder(Codecs.integer())
					.observableList(subject)
					.build())
			.build();
		
		// serve
		server.start();
		
		// connect
		Observable<Integer> oc = RemoteObservable.connect("localhost", port, Codecs.integer());
		// assert
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(20200, t1.intValue()); // sum of number 0-200
			}
		});
	}
	
	@Test
	public void testServeListObservablesThenObserveNewList(){
		// setup
		Observable<Integer> os1 = Observable.range(0, 101);
		Observable<Integer> os2 = Observable.range(100, 101);
		List<Observable<Integer>> toServe = new LinkedList<Observable<Integer>>();
		toServe.add(os1);
		toServe.add(os2);
		List<Observable<Integer>> initialValue = new ArrayList<Observable<Integer>>(0);
		BehaviorSubject <List<Observable<Integer>>> subject = BehaviorSubject.create(initialValue);
		subject.onNext(toServe);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		
		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(serverPort)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.encoder(Codecs.integer())
					.observableList(subject)
					.build())
			.build();
		server.start();
				
		// connect
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		// assert
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(20200, t1.intValue()); // sum of number 0-200
			}
		});
		// add new list
		Observable<Integer> os11 = Observable.range(0, 51);
		Observable<Integer> os22 = Observable.range(51, 50);
		List<Observable<Integer>> newList = new LinkedList<Observable<Integer>>();
		newList.add(os11);
		newList.add(os22);
		subject.onNext(newList);
		Observable<Integer> oc1 = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		Observable.sumInteger(oc1).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-200
			}
		});
	}
	
	@Test
	public void testHashCodeSlottingServer(){
		// setup
		Observable<Integer> os = Observable.range(0, 101);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		
		RemoteObservableServer server = new RemoteObservableServer.Builder()
		.port(serverPort)
		.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
				.encoder(Codecs.integer())
				.observable(os)
				.slottingStrategy(SlottingStrategies.<Integer>hashCodeSlotting(2))
				.build())
		.build();
		server.start();
		
		// connect with 2 remotes
		Observable<Integer> oc1 = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		Observable<Integer> oc2 = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		// merge results
		Observable<Integer> merged = Observable.merge(oc1,oc2);
		// assert
		Observable.sumInteger(merged).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-100
			}
		});
		
	}
	
	@Test
	public void testChainedRemoteObservables() throws InterruptedException{
		
		// first node
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		Observable<Integer> os = Observable.range(0, 100);
		
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serve(serverPort, "source", os, Codecs.integer())
			.start();
		
		// middle node, receiving input from first node
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, "source", Codecs.integer());
		// transform stream from first node
		Observable<Integer> transformed = oc.map(new Func1<Integer,Integer>(){
			@Override
			public Integer call(Integer t1) {
				return t1+1; // shift sequence by one
			}
		});
		int serverPort2 = portSelector.acquirePort();
		RemoteObservable.serve(serverPort2, "transformed", transformed, Codecs.integer())
			.start();
		
		// connect to second node
		Observable<Integer> oc2 = RemoteObservable.connect("localhost", serverPort2, "transformed", Codecs.integer());		
		
		Observable.sumInteger(oc2).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-100
			}
		});		
	}
	
	@Test(expected=RuntimeException.class)
	public void testError(){
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		Observable<Integer> os = Observable.create(new OnSubscribe<Integer>(){
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				subscriber.onNext(1);
				subscriber.onError(new Exception("test-exception"));
			}
		});
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serve(serverPort, os, Codecs.integer())
			.start();
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-100
			}
		});		
	}
	
	@Test
	public void testUnsubscribeForRemoteObservable() throws InterruptedException{
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		
		// serve up first observable
		final MutableReference<Boolean> sourceSubscriptionUnsubscribed = new MutableReference<Boolean>();
		Observable<Integer> os = Observable.create(new OnSubscribe<Integer>(){
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				int i=0;
				sourceSubscriptionUnsubscribed.setValue(subscriber.isUnsubscribed());
				while(!sourceSubscriptionUnsubscribed.getValue()){
					subscriber.onNext(i++);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					sourceSubscriptionUnsubscribed.setValue(subscriber.isUnsubscribed());
				}
			}
		});
		
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serve(serverPort, os, Codecs.integer())
			.start();
		
		// connect to remote observable
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		Subscription sub = oc.subscribe();
		
		Assert.assertEquals(false, sub.isUnsubscribed());
		Thread.sleep(1000); // allow a few iterations
		sub.unsubscribe();
		Assert.assertEquals(true, sub.isUnsubscribed());
//		Assert.assertEquals(true, sourceSubscriptionUnsubscribed.getValue()); // TODO fails due to blocking nature of OnSubscribe call
	}
	
	@Test
	public void testUnsubscribeForChainedRemoteObservable() throws InterruptedException{
		
		// serve first node in chain
		final MutableReference<Boolean> sourceSubscriptionUnsubscribed = new MutableReference<Boolean>();
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		Observable<Integer> os = Observable.create(new OnSubscribe<Integer>(){
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				int i=0;
				sourceSubscriptionUnsubscribed.setValue(subscriber.isUnsubscribed());
				while(!sourceSubscriptionUnsubscribed.getValue()){
					subscriber.onNext(i++);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					sourceSubscriptionUnsubscribed.setValue(subscriber.isUnsubscribed());
				}
			}
		});
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serve(serverPort, os, Codecs.integer())
			.start();
		
		// serve second node in chain, using first node's observable
		Observable<Integer> oc1 = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		int serverPort2 = portSelector.acquirePort();
		RemoteObservable.serve(serverPort2, oc1, Codecs.integer())
			.start();
		
		// connect to second node
		Observable<Integer> oc2 = RemoteObservable.connect("localhost", serverPort2, Codecs.integer());
		
		Subscription subscription = oc2.subscribe();

		// check client subscription
		Assert.assertEquals(false, subscription.isUnsubscribed());
		
		Thread.sleep(1000); // allow a few iterations to complete
		
		// unsubscribe to client subscription
		subscription.unsubscribe();
		// check client
		Assert.assertEquals(true, subscription.isUnsubscribed());
		// check source
//		Assert.assertEquals(true, sourceSubscriptionUnsubscribed.getValue()); // TODO fails due to blocking nature of OnSubscribe call
	}
	
	@Test
	public void testSubscribeParametersByFilteringOnServer(){
		
		// setup
		Observable<Integer> os = Observable.range(0, 101);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(serverPort)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.encoder(Codecs.integer())
					.observable(os)
					.serverSideFilter(ServerSideFilters.oddsAndEvens())
					.build())
			.build();
		server.start();
		
		// connect
		Map<String,String> subscribeParameters = new HashMap<String,String>();
		subscribeParameters.put("type", "even");
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, subscribeParameters, Codecs.integer());
		// assert
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(2550, t1.intValue()); // sum of number 0-100
			}
		});	
	}
	
	
	
}
