package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;

/**
 * @author Nitesh Kant
 */
public class HttpErrorHandlerTest {

    public static final String X_TEST_RESP_GEN_CALLED_HEADER_NAME = "X-TEST-RESP-GEN-CALLED";

    private RxServer<HttpRequest<ByteBuf>, HttpResponse<ByteBuf>> server;

    @After
    public void tearDown() throws Exception {
        if (null != server) {
            server.shutdown();
        }
    }

    @Test
    public void testErrorGenerator() throws Exception {
        int port = 9999;
        server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(
                    HttpRequest<ByteBuf> request,
                    HttpResponse<ByteBuf> response) {
                return Observable
                        .error(new IllegalStateException(
                                "I always throw an error."));
            }
        }).withErrorResponseGenerator(new ErrorResponseGenerator<ByteBuf>() {
            @Override
            public void updateResponse(
                    HttpResponse<ByteBuf> response,
                    Throwable error) {
                response.setStatus(
                        HttpResponseStatus.INTERNAL_SERVER_ERROR);
                response.getHeaders().add(
                        X_TEST_RESP_GEN_CALLED_HEADER_NAME,
                        "true");
            }
        }).start();

        io.reactivex.netty.protocol.http.client.HttpRequest<ByteBuf> request =
                io.reactivex.netty.protocol.http.client.HttpRequest.createGet("/");

        io.reactivex.netty.protocol.http.client.HttpResponse<ByteBuf> response =
                RxNetty.createHttpClient("localhost", port).submit(request).toBlockingObservable().last();

        Assert.assertEquals("Unexpected response status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                            response.getStatus().code());
        Assert.assertTrue("Error response generator not called.", response.getHeaders().contains(
                X_TEST_RESP_GEN_CALLED_HEADER_NAME));
    }

    @Test
    public void testErrorGeneratorThrowException() throws Exception {
        int port = 9998;
        server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpRequest<ByteBuf> request, HttpResponse<ByteBuf> response) {
                throw new IllegalStateException("I always throw an error.");
            }
        }).withErrorResponseGenerator(new ErrorResponseGenerator<ByteBuf>() {
            @Override
            public void updateResponse(HttpResponse<ByteBuf> response, Throwable error) {
                response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                response.getHeaders().add(X_TEST_RESP_GEN_CALLED_HEADER_NAME, "true");
            }
        }).start();

        io.reactivex.netty.protocol.http.client.HttpRequest<ByteBuf> request =
                io.reactivex.netty.protocol.http.client.HttpRequest.createGet("/");

        io.reactivex.netty.protocol.http.client.HttpResponse<ByteBuf> response =
                RxNetty.createHttpClient("localhost", port).submit(request).toBlockingObservable().last();

        Assert.assertEquals("Unexpected response status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                            response.getStatus().code());
        Assert.assertTrue("Error response generator not called.", response.getHeaders().contains(
                X_TEST_RESP_GEN_CALLED_HEADER_NAME));
    }
}
