package io.reactivex.netty.examples.http.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.reactivex.netty.examples.http.ssl.SslHelloWorldServer.DEFAULT_PORT;


/**
 * @author Tomasz Bak
 */
public class SslHelloWorldTest {


    private HttpServer<ByteBuf, ByteBuf> server;

    @Before
    public void setupHttpHelloServer() throws Exception {
        server = new SslHelloWorldServer(DEFAULT_PORT).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testRequestReplySequence() throws Exception {
        SslHelloWorldClient client = new SslHelloWorldClient(DEFAULT_PORT);
        HttpResponseStatus responseStatus = client.sendHelloRequest();
        Assert.assertEquals(HttpResponseStatus.OK, responseStatus);
    }
}
