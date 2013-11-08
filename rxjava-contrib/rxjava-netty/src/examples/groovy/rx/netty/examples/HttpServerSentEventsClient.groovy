package rx.netty.examples

import rx.Observable
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.protocol.http.ObservableHttpResponse;

class HttpServerSentEventsClient {

    public static void main(String[] args) {

        RxNetty.createHttpRequest("http://ec2-107-22-122-75.compute-1.amazonaws.com:7001/turbine.stream?cluster=api-prod-c0us.ca")
                .flatMap({ ObservableHttpResponse response ->
                    println("Received connection: " + response.response().getStatus());
                    return response.content();
                })
                .take(10)
                .toBlockingObservable().forEach({ println(it)});
    }
}
