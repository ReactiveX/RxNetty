package io.reactivex.netty.examples.http.helloworld;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.reactivex.netty.examples.http.helloworld.HelloWorldServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class HelloWorldTest {

    private HttpServer<ByteBuf, ByteBuf> server;

    @Before
    public void setupHttpHelloServer() {
        server = new HelloWorldServer(DEFAULT_PORT).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testRequestReplySequence() {
        HelloWorldClient client = new HelloWorldClient(DEFAULT_PORT);
        HttpResponseStatus statusCode = client.sendHelloRequest();
        Assert.assertEquals(HttpResponseStatus.OK, statusCode);
    }
}
