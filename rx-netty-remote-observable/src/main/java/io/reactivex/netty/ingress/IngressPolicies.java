package io.reactivex.netty.ingress;

import io.reactivex.netty.RemoteRxEvent;
import io.reactivex.netty.channel.ObservableConnection;

import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.subjects.ReplaySubject;

public class IngressPolicies {

	private IngressPolicies(){}
	
	public static IngressPolicy whiteListPolicy(Observable<Set<String>> whiteList){
		return new InetAddressWhiteListIngressPolicy(whiteList);
	}
	public static IngressPolicy allowOnlyLocalhost(){
		ReplaySubject<Set<String>> subject = ReplaySubject.create();
		Set<String> list = new HashSet<String>();
		list.add("127.0.0.1");
		subject.onNext(list);
		return new InetAddressWhiteListIngressPolicy(subject);
	}
	public static IngressPolicy allowAll(){
		return new IngressPolicy(){
			@Override
			public boolean allowed(
					ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
				return true;
			}
		};
	}
}
