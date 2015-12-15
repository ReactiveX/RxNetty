package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DisbleAutoReleaseTest {

    @Test(timeout = 60000)
    public void testDisableAutoRelease() throws Exception {

        final List<ByteBuf> requestBufs = new ArrayList<ByteBuf>();

        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return request.getContent()
                              .map(new Func1<ByteBuf, Void>() {
                                  @Override
                                  public Void call(ByteBuf byteBuf) {
                                      requestBufs.add(byteBuf);
                                      return null;
                                  }
                              })
                              .ignoreElements()
                              .cast(Void.class)
                              .concatWith(response.writeStringAndFlush("Welcome!"));
            }
        }).disableAutoReleaseBuffers().enableWireLogging(LogLevel.DEBUG).build().start();

        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
                                                      .submit(HttpClientRequest.createPost("/").withContent("Hello"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", response.getStatus(), HttpResponseStatus.OK);

        Assert.assertEquals("Unexpected request content on server.", 1, requestBufs.size());
        Assert.assertEquals("Unexpected request content buffer ref count.", 1, requestBufs.get(0).refCnt());

        requestBufs.get(0).release();
    }
}
