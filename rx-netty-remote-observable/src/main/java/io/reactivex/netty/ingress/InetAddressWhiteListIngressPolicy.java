package io.reactivex.netty.ingress;

import io.reactivex.netty.RemoteRxEvent;
import io.reactivex.netty.channel.ObservableConnection;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.functions.Action1;

public class InetAddressWhiteListIngressPolicy implements IngressPolicy{

	private AtomicReference<Set<String>> whiteList = new AtomicReference<Set<String>>();
	
	InetAddressWhiteListIngressPolicy(Observable<Set<String>> allowedIpAddressesObservable) {
		allowedIpAddressesObservable.subscribe(new Action1<Set<String>>(){
			@Override
			public void call(Set<String> newList) {
				whiteList.set(newList);
			}
		});
	}

	@Override
	public boolean allowed(
			ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
		InetSocketAddress inetSocketAddress 
			= (InetSocketAddress) connection.getChannelHandlerContext().channel().remoteAddress();
		return whiteList.get().contains(inetSocketAddress.getAddress().getHostAddress());
	}
		
}
