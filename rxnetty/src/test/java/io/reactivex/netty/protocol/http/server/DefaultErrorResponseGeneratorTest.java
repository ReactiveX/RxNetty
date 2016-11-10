package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.nio.charset.Charset;

public class DefaultErrorResponseGeneratorTest {
    private RxServer<HttpServerRequest<ByteBuf>, HttpServerResponse<ByteBuf>> server;

    @After
    public void tearDown() throws Exception {
        if (null != server) {
            server.shutdown();
        }
    }

    @Test
    public void testErrorGenerator() throws Exception {
        server = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(
                    HttpServerRequest<ByteBuf> request,
                    HttpServerResponse<ByteBuf> response) {
                return Observable
                        .error(new IllegalStateException(
                                "I always throw an error<>'&\"{}."));
            }
        }).start();

        int port = server.getServerPort();

        HttpClientRequest<ByteBuf> request =
                HttpClientRequest.createGet("/");

        String html =
                RxNetty.createHttpClient("localhost", port).submit(request)
                        .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
                            @Override
                            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                                return response.getContent().map(new Func1<ByteBuf, String>() {
                                    @Override
                                    public String call(ByteBuf byteBuf) {
                                        return byteBuf.toString(Charset.forName("ASCII"));
                                    }
                                }).reduce(new Func2<String, String, String>() {
                                    @Override
                                    public String call(String s, String s2) {
                                        return s + s2;
                                    }
                                });
                            }
                        }).toBlocking().last();

        Assert.assertThat("Unexpected response status",
                html,
                JUnitMatchers.containsString("I always throw an error&lt;&gt;&#39;&amp;&quot;&#123;&#125;.\n"));
    }
}