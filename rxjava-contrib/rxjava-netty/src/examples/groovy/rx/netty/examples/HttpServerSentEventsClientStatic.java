package rx.netty.examples;

import rx.Observable;
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.protocol.http.HttpProtocolHandler;
import rx.netty.experimental.protocol.http.Message;
import rx.netty.experimental.protocol.http.ObservableHttpResponse;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class HttpServerSentEventsClientStatic {

    public static void main(String[] args) {

        RxNetty.createHttpRequest("http://ec2-107-22-122-75.compute-1.amazonaws.com:7001/turbine.stream?cluster=api-prod-c0us.ca", HttpProtocolHandler.SSE_HANDLER)
                .flatMap(new Func1<ObservableHttpResponse<Message>, Observable<Message>>() {

                    @Override
                    public Observable<Message> call(ObservableHttpResponse<Message> response) {
                        System.out.println("Received connection: " + response.response().getStatus());
                        return response.content();
                    }

                })
                .take(10)
                .toBlockingObservable().forEach(new Action1<Message>() {

                    @Override
                    public void call(Message message) {
                        System.out.println("Message => " + message.getEventData().trim());
                    }

                });
    }
}
