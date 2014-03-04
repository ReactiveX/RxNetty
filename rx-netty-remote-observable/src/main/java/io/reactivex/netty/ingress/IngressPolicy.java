package io.reactivex.netty.ingress;

import io.reactivex.netty.RemoteRxEvent;
import io.reactivex.netty.channel.ObservableConnection;

public interface IngressPolicy {
	public boolean allowed(ObservableConnection<RemoteRxEvent,RemoteRxEvent> connection);

}
