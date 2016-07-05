package io.reactivex.netty.protocol.http.client.events;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRule;
import io.reactivex.netty.test.util.MockClientEventListener.ClientEvent;
import io.reactivex.netty.test.util.MockConnectionEventListener.Event;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;

public class HttpClientEventsTest {

    @Rule
    public final HttpServerRule serverRule = new HttpServerRule();

    @Test(timeout = 60000)
    public void testEventsPublished() throws Exception {
        HttpClientEventsListenerImpl listener = sendRequests(false);

        listener.getTcpDelegate().assertMethodCalled(ClientEvent.ConnectStart);
        listener.getTcpDelegate().assertMethodCalled(ClientEvent.ConnectSuccess);
        listener.getTcpDelegate().assertMethodCalled(Event.WriteStart);
        listener.getTcpDelegate().assertMethodCalled(Event.WriteSuccess);
        listener.getTcpDelegate().assertMethodCalled(Event.FlushStart);
        listener.getTcpDelegate().assertMethodCalled(Event.FlushSuccess);
        listener.getTcpDelegate().assertMethodCalled(Event.BytesRead);
    }

    @Test(timeout = 60000)
    public void testPooledEventsPublished() throws Exception {
        HttpClientEventsListenerImpl listener = sendRequests(true);

        listener.getTcpDelegate().assertMethodCalled(ClientEvent.AcquireStart);
        listener.getTcpDelegate().assertMethodCalled(ClientEvent.AcquireSuccess);
        listener.getTcpDelegate().assertMethodCalled(ClientEvent.ConnectStart);
        listener.getTcpDelegate().assertMethodCalled(ClientEvent.ConnectSuccess);
        listener.getTcpDelegate().assertMethodCalled(Event.WriteStart);
        listener.getTcpDelegate().assertMethodCalled(Event.WriteSuccess);
        listener.getTcpDelegate().assertMethodCalled(Event.FlushStart);
        listener.getTcpDelegate().assertMethodCalled(Event.FlushSuccess);
        listener.getTcpDelegate().assertMethodCalled(Event.BytesRead);
    }

    protected HttpClientEventsListenerImpl sendRequests(boolean pool) {
        serverRule.startServer();
        HttpClientEventsListenerImpl listener = new HttpClientEventsListenerImpl();
        if (pool) {
            SingleHostPoolingProviderFactory<ByteBuf, ByteBuf> provider =
                    SingleHostPoolingProviderFactory.createBounded(10);
            Host host = new Host(serverRule.getServerAddress());
            serverRule.setupClient(HttpClient.newClient(provider, Observable.just(host)));

        }
        serverRule.getClient().subscribe(listener);
        HttpClientRequest<ByteBuf, ByteBuf> request = serverRule.getClient().createGet("/");

        HttpClientResponse<ByteBuf> resp = serverRule.sendRequest(request);
        serverRule.assertResponseContent(resp);
        return listener;
    }
}
