package io.reactivex.netty.examples.java;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.http.HttpClient;
import io.reactivex.netty.http.ObservableHttpResponse;
import rx.Observer;
import rx.util.functions.Action1;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class RawHttpClient {

    public static void main(String[] args) {
        HttpClient<FullHttpRequest, HttpObject> client = RxNetty.createHttpClient("localhost", 8080);
        client.connectAndObserve(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hello"))
              .subscribe(new Observer<ObservableHttpResponse<HttpObject>>() {
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
                   public void onNext(ObservableHttpResponse<HttpObject> observableResponse) {
                       observableResponse.header().subscribe(new Action1<HttpResponse>() {
                           @Override
                           public void call(HttpResponse response) {
                               System.out.println("New response recieved.");
                               System.out.println("========================");
                               System.out.println(
                                       response.getProtocolVersion().text() + ' ' + response.getStatus().code()
                                       + ' ' + response.getStatus().reasonPhrase());
                               for (Map.Entry<String, String> aHeader : response.headers().entries()) {
                                   System.out.println(aHeader.getKey() + ": " + aHeader.getValue());
                               }
                           }
                       });
                       observableResponse.content().subscribe(new Action1<HttpObject>() {
                           @Override
                           public void call(HttpObject response) {
                               if (HttpContent.class.isAssignableFrom(response.getClass())) {
                                   HttpContent content = (HttpContent) response;
                                   System.out.print(content.content().toString(Charset.defaultCharset()));
                                   if (LastHttpContent.class.isAssignableFrom(response.getClass())) {
                                       System.out.println("========================");
                                   }
                               }
                           }
                       });
                   }
               });
    }
}
