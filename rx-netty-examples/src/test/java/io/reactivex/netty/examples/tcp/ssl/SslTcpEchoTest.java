package io.reactivex.netty.examples.tcp.ssl;

import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.reactivex.netty.examples.tcp.ssl.SslTcpEchoServer.DEFAULT_PORT;


/**
 * @author Tomasz Bak
 */
public class SslTcpEchoTest {
    private RxServer server;

    @Before
    public void setupServer() {
        server = new SslTcpEchoServer(DEFAULT_PORT).createServer();
        server.start();
    }

    @After
    public void stopServer() throws Exception {
        server.shutdown();
    }

    @Test
    public void testRequestReplySequence() {
        SslTcpEchoClient client = new SslTcpEchoClient(DEFAULT_PORT);
        List<String> reply = client.sendEchos();
        Assert.assertEquals(10, reply.size());
    }
}
