package io.reactivex.netty.examples.java;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.HttpClient;
import io.reactivex.netty.protocol.http.ObservableHttpResponse;
import io.reactivex.netty.protocol.text.sse.SSEEvent;
import rx.Observer;
import rx.util.functions.Action1;

import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class HttpSseClient {

    public static void main(String[] args) {
        HttpClient<FullHttpRequest,SSEEvent> sseClient = RxNetty.createSseClient("localhost", 8080);
        sseClient.submit(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hello"))
                 .subscribe(new Observer<ObservableHttpResponse<SSEEvent>>() {
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
                     public void onNext(ObservableHttpResponse<SSEEvent> observableResponse) {
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
                         observableResponse.content().subscribe(new Action1<SSEEvent>() {
                             @Override
                             public void call(SSEEvent event) {
                                 System.out.println(event.getEventName() + ": " + event.getEventData());
                             }
                         });
                     }
                 });
    }
}
