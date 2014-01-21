/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.http;

import io.netty.handler.codec.http.HttpResponse;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

/**
 * The envelope object that holds both observables of the content of an HTTP response,
 * and the initial response object.
 * 
 * @param <T>
 *            The type of HTTP response
 */
public class ObservableHttpResponse<T> {
    private final HttpResponse response;
    private final PublishSubject<T> subject;

    public enum Events {
        End    
    }
    
    public ObservableHttpResponse(HttpResponse response, PublishSubject<T> subject) {
        this.response = response;
        this.subject = subject;
    }

    public Observable<T> content() {
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