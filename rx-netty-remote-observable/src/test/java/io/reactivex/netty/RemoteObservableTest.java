package io.reactivex.netty;

import io.reactivex.netty.codec.Codecs;
import io.reactivex.netty.slotting.SlottingStrategies;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
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
		RemoteObservable.serve(serverPort, os, Codecs.integer());
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
	public void testServeListObservables(){
		// setup
		Observable<Integer> os1 = Observable.range(0, 101);
		Observable<Integer> os2 = Observable.range(100, 101);
		List<Observable<Integer>> toServe = new LinkedList<Observable<Integer>>();
		toServe.add(os1);
		toServe.add(os2);
		ReplaySubject <List<Observable<Integer>>> subject = ReplaySubject.create();
		subject.onNext(toServe);
		// serve
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serveMany(serverPort, subject, Codecs.integer());
		// connect
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
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
		RemoteObservable.serveMany(serverPort, subject, Codecs.integer());
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
		RemoteObservable.serve(serverPort, os, Codecs.integer()
				, SlottingStrategies.<Integer>hashCodeSlotting(2)); // 2 slots, for two connections
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
		Observable<Integer> os = Observable.range(0, 101);
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serve(serverPort, os, Codecs.integer());
		
		// middle node, receiving input from first node
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		// transform stream from first node
		Observable<Integer> transformed = oc.map(new Func1<Integer,Integer>(){
			@Override
			public Integer call(Integer t1) {
				return t1+1; // shift sequence by one
			}
		});
		int serverPort2 = portSelector.acquirePort();
		RemoteObservable.serve(serverPort2, transformed, Codecs.integer());
		// connect to second node
		Observable<Integer> oc2 = RemoteObservable.connect("localhost", serverPort2, Codecs.integer());
		
		Observable.sumInteger(oc2).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5151, t1.intValue()); // sum of number 0-100
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
		RemoteObservable.serve(serverPort, os, Codecs.integer());
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		Observable.sumInteger(oc).toBlockingObservable().forEach(new Action1<Integer>(){
			@Override
			public void call(Integer t1) {
				Assert.assertEquals(5050, t1.intValue()); // sum of number 0-100
			}
		});		
	}
	
	@Test
	public void testUnsubscribeForSingleNode() throws InterruptedException{
		PortSelectorWithinRange portSelector = new PortSelectorWithinRange(8000, 9000);
		Observable<Integer> os = Observable.range(0, 10000); // long sequence
		int serverPort = portSelector.acquirePort();
		RemoteObservable.serve(serverPort, os, Codecs.integer());
		Observable<Integer> oc = RemoteObservable.connect("localhost", serverPort, Codecs.integer());
		Subscription sub = oc.subscribe();
		Assert.assertEquals(true, !sub.isUnsubscribed()); // timing of check is a race against complete
		sub.unsubscribe();
		Assert.assertEquals(true, sub.isUnsubscribed()); // timing of check is a race against complete
	}
	
	
	
}
