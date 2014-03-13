package io.reactivex.netty;

import io.reactivex.netty.codec.Codecs;
import io.reactivex.netty.filter.ServerSideFilters;
import io.reactivex.netty.slotting.SlottingStrategies;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
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
	public void testServedMergedObservables(){
		// setup
		Observable<Integer> os1 = Observable.range(0, 101);
		Observable<Integer> os2 = Observable.range(100, 101);
		ReplaySubject<Observable<Integer>> subject = ReplaySubject.create();
		subject.onNext(os1);
		subject.onNext(os2);
		subject.onCompleted();
		
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int port = portSelector.acquirePort();
		
		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(port)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.encoder(Codecs.integer())
					.observable(Observable.merge(subject))
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
	public void testServedMergedObservablesAddAfterServe(){
		// setup
		Observable<Integer> os1 = Observable.range(0, 100);
		Observable<Integer> os2 = Observable.range(100, 100);
		ReplaySubject<Observable<Integer>> subject = ReplaySubject.create();
		subject.onNext(os1);
		subject.onNext(os2);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		
		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(serverPort)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.encoder(Codecs.integer())
					.observable(Observable.merge(subject))
					.build())
			.build();
		server.start();
		
		// add after serve
		Observable<Integer> os3 = Observable.range(200, 101);
		subject.onNext(os3);
		subject.onCompleted();
				
		// connect
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		// assert
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(45150, t1.intValue()); // sum of number 0-200
			}
		});
	}
	
	@Test
	public void testServedMergedObservablesAddAfterConnect(){
		// setup
		Observable<Integer> os1 = Observable.range(0, 100);
		Observable<Integer> os2 = Observable.range(100, 100);
		ReplaySubject<Observable<Integer>> subject = ReplaySubject.create();
		subject.onNext(os1);
		subject.onNext(os2);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		
		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(serverPort)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.encoder(Codecs.integer())
					.observable(Observable.merge(subject))
					.build())
			.build();
		server.start();
		
		// add after serve
		Observable<Integer> os3 = Observable.range(200, 100);
		subject.onNext(os3);
				
		// connect
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		
		// add after connect
		Observable<Integer> os4 = Observable.range(300, 101);
		subject.onNext(os4);
		subject.onCompleted();
		
		// assert
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(80200, t1.intValue()); // sum of number 0-200
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
		}).subscribeOn(Schedulers.io());
		
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serve(serverPort, os, Codecs.integer())
			.start();
		
		// connect to remote observable
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		Subscription sub = oc.subscribe();
		
		Assert.assertEquals(false, sub.isUnsubscribed());
		Thread.sleep(1000); // allow a few iterations
		sub.unsubscribe();
		Thread.sleep(1000); // allow time for unsubscribe to propagate
		Assert.assertEquals(true, sub.isUnsubscribed());
		Assert.assertEquals(true, sourceSubscriptionUnsubscribed.getValue()); // TODO fails due to blocking nature of OnSubscribe call
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
					System.out.println(i);
					System.out.println(subscriber.isUnsubscribed());
					sourceSubscriptionUnsubscribed.setValue(subscriber.isUnsubscribed());
				}
				System.out.println("outcome"+sourceSubscriptionUnsubscribed.getValue());
				System.out.println("stoppped");
			}
		}).subscribeOn(Schedulers.io());
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
		Thread.sleep(1000); // allow time for unsubscribe to propagate
		// check client
		Assert.assertEquals(true, subscription.isUnsubscribed());
		// check source
		Assert.assertEquals(true, sourceSubscriptionUnsubscribed.getValue()); // TODO fails due to blocking nature of OnSubscribe call
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
	
	@Test
	public void testOnCompletedFromReplaySubject(){
		PublishSubject<Integer> subject = PublishSubject.create();
		subject.onNext(1);
		subject.onNext(2);
		subject.onNext(3);
		subject.onCompleted();
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		RemoteObservableServer server = new RemoteObservableServer.Builder()
			.port(serverPort)
			.addObservable(new RemoteObservableConfiguration.Builder<Integer>()
					.encoder(Codecs.integer())
					.observable(subject)
					.serverSideFilter(ServerSideFilters.oddsAndEvens())
					.build())
			.build();
		server.start();
		// connect
		Observable<Integer> ro = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		final MutableReference<Boolean> completed = new MutableReference<Boolean>();
		ro.materialize().toBlockingObservable().forEach(new Action1<Notification<Integer>>(){
			@Override
			public void call(Notification<Integer> notification) {
				if (notification.isOnCompleted()){
					completed.setValue(true);
				}
			}
		});
		Assert.assertEquals(true, completed.getValue());
	}
	
}
