package io.reactivex.netty;

import io.reactivex.netty.channel.ObservableConnection;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;


public class RemoteUnsubscribe implements Subscription{

	private ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection;
	private BooleanSubscription subscription = new BooleanSubscription();
	
	@Override
	public void unsubscribe() {
		if (connection != null){
			connection.write(RemoteRxEvent.unsubscribed()); // write unsubscribe event to server
		}
		subscription.unsubscribe();
	}

	@Override
	public boolean isUnsubscribed() {
		return subscription.isUnsubscribed();
	}

	void setConnection(
			ObservableConnection<RemoteRxEvent, RemoteRxEvent> connection) {
		this.connection = connection;
	}

}
