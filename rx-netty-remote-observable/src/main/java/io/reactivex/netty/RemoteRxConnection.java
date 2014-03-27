package io.reactivex.netty;

import rx.Observable;

public class RemoteRxConnection<T> {

	private Observable<T> observable;
	private ConnectionMetrics metrics;
	
	public RemoteRxConnection(Observable<T> observable,
			ConnectionMetrics metrics) {
		this.observable = observable;
		this.metrics = metrics;
	}

	public Observable<T> getObservable() {
		return observable;
	}

	public ConnectionMetrics getMetrics() {
		return metrics;
	}
}
