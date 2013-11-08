package rx.netty.experimental.protocol.http;

import io.netty.handler.codec.http.HttpResponse;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

/**
 * The envelope object that holds both observables of the content of an HTTP response,
 * and the initial response object.
 *
 * @param <T> The type of HTTP response
 */
public class ObservableHttpResponse<T> {
    private final HttpResponse response;
    private final PublishSubject<T> subject;

    public ObservableHttpResponse(HttpResponse response, PublishSubject<T> subject) {
        this.response = response;
        this.subject = subject;
    }

    public Observable<T> content(){
        return subject;
    }

    public HttpResponse response() {
        return response;
    }

    Observer<T> contentObserver() {
        return new Observer<T>() {
            public synchronized void onCompleted() {
                subject.onCompleted();
            }

            public synchronized void onError(Throwable e) {
                subject.onError(e);
            }

            public void onNext(T o) {
                subject.onNext(o);
            }
        };
    }
}