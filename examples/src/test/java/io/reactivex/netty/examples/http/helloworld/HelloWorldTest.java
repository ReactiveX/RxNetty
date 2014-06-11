package io.reactivex.netty.examples.http.helloworld;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HelloWorldTest {

    private static final int PORT = 8090;

    private Thread server;

    @Before
    public void setupHttpHelloServer() {
        server = new Thread(new Runnable() {
            @Override
            public void run() {
                HelloWorldServer.main(new String[]{Integer.toString(PORT)});
            }
        });
        server.start();
    }

    @After
    public void stopServer() {
        if (server != null) {
            server.interrupt();
        }
    }

    @Test
    public void testRequestReplySequence() {
        HelloWorldClient client = new HelloWorldClient(PORT);
        client.sendHelloRequest();
        Assert.assertEquals(client.lastResponse.getStatus(), HttpResponseStatus.OK);
    }
}
