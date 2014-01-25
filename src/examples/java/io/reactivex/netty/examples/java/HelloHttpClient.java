package io.reactivex.netty.examples.java;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.ObservableHttpResponse;
import rx.Observer;
import rx.util.functions.Action1;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class HelloHttpClient {

    public static void main(String[] args) {
        RxNetty.createHttpClient("localhost", 8080)
               .submit(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hello"))
               .subscribe(new Observer<ObservableHttpResponse<FullHttpResponse>>() {
                   @Override
                   public void onCompleted() {
                       System.out.println("Response complete.");
                   }

                   @Override
                   public void onError(Throwable e) {
                       System.out.println("Error occured while sending/recieving Http request/response. Error: ");
                       e.printStackTrace(System.out);
                   }

                   @Override
                   public void onNext(ObservableHttpResponse<FullHttpResponse> observableResponse) {
                       observableResponse.content().subscribe(new Action1<FullHttpResponse>() {
                           @Override
                           public void call(FullHttpResponse response) {
                               System.out.println("New response recieved.");
                               System.out.println("========================");
                               System.out.println(
                                       response.getProtocolVersion().text() + ' ' + response.getStatus().code()
                                       + ' ' + response.getStatus().reasonPhrase());
                               for (Map.Entry<String, String> header : response.headers().entries()) {
                                   System.out.println(header.getKey() + ": " + header.getValue());
                               }
                               System.out.print(response.content().toString(Charset.defaultCharset()));
                               System.out.println("========================");
                           }
                       });
                   }
               });
    }
}
