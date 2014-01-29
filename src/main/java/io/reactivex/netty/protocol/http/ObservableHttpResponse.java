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
package io.reactivex.netty.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

/**
 * Represents an Http response as an observable. <br/>
 *
 * A raw Http response has the following parts:
 * <ul>
 <li>Header: Contains the first HTTP line & HTTP headers. Netty's event object: {@link HttpResponse}</li>
 <li>One or more content chunks.  Netty's event object: {@link HttpContent}</li>
 <li>Terminating HTTP content chunk that may contain headers. Netty's event object: {@link LastHttpContent}</li>
 </ul>
 *
 * The following table details the correlation between HTTP response parts and the different {@link Observable} obtained
 * from this class.:
 *
 * <h1>Netty pipeline emits raw HTTP response.</h1>
 * <table border="1">
 *     <tr>
 *         <td>Raw Http Part</td>
 *         <td>How to access from {@link ObservableHttpResponse}</td>
 *     </tr>
 *     <tr>
 *         <td>Header</td>
 *         <td>{@link ObservableHttpResponse#header()}. This will contain exactly one {@link Observer#onNext(Object)}
 *         call per request. {@link Observer#onCompleted()} will be called when the entire response is completed.</td>
 *     </tr>
 *     <tr>
 *         <td>Content</td>
 *         <td>{@link ObservableHttpResponse#content()}. Every new content will invoke {@link Observer#onNext(Object)}
 *         on the subscribers. {@link Observer#onCompleted()} will be called when the entire response is completed.</td>
 *     </tr>
 *     <tr>
 *         <td>Terminating Content</td>
 *         <td>{@link ObservableHttpResponse#content()} will be invoked for the content part of this data. The trailing
 *         headers are not passed to any subscriber. This will also invoke {@link Observer#onCompleted()} on both
 *         header and content observers.</td>
 *     </tr>
 * </table>
 *
 * <h1>Netty pipeline emits {@link FullHttpResponse}.</h1>
 * <table border="1">
 *     <tr>
 *         <td>Raw Http Part</td>
 *         <td>How to access from {@link ObservableHttpResponse}</td>
 *     </tr>
 *     <tr>
 *         <td>Header</td>
 *         <td>{@link ObservableHttpResponse#header()}. This will contain exactly one {@link Observer#onNext(Object)}
 *         call per response. {@link Observer#onCompleted()} will be called immediately.</td>
 *     </tr>
 *     <tr>
 *         <td>Content</td>
 *         <td>{@link ObservableHttpResponse#content()}. This will contain exactly one {@link Observer#onNext(Object)}
 *         call per response. {@link Observer#onCompleted()} will be called immediately.</td>
 *     </tr>
 *     <tr>
 *         <td>Terminating Content</td>
 *         <td>{@link ObservableHttpResponse#content()}. The entire content will be provided in one invocation of
 *         {@link Observer#onNext(Object)} of the content observable. The trailing headers are not passed to any
 *         subscriber. {@link Observer#onCompleted()} will be called immediately.</td>
 *     </tr>
 * </table>
 *
 * <h1>Netty pipeline emits a custom object.</h1>
 * <table border="1">
 *     <tr>
 *         <td>Raw Http Part</td>
 *         <td>How to access from {@link ObservableHttpResponse}</td>
 *     </tr>
 *     <tr>
 *         <td>Header</td>
 *         <td>Iff the pipeline emits HttpResponse objects, {@link #header()} can be used to get the header.
 *         {@link #header()}'s {@link Observer#onCompleted()} will be called after the connection is closed.</td>
 *     </tr>
 *     <tr>
 *         <td>Content</td>
 *         <td>{@link ObservableHttpResponse#content()}. This will contain the custom user object.
 *         {@link Observer#onCompleted()} will be called after the connection is closed.</td>
 *     </tr>
 *     <tr>
 *         <td>Terminating Content</td>
 *         <td>{@link ObservableHttpResponse#content()}. will be invoked for the content part of this data. The trailing
 *         headers are not passed to any subscriber. {@link Observer#onCompleted()} will be called after the connection
 *         is closed.</td>
 *     </tr>
 * </table>
 *
 * @param <T>
 */
public class ObservableHttpResponse<T> {

    private final PublishSubject<HttpResponse> headerSubject;
    private final PublishSubject<T> contentSubject;

    public <I extends HttpRequest> ObservableHttpResponse(final ObservableConnection<T, I> observableConnection,
                                                          Observer<? super ObservableHttpResponse<T>> requestProcessingObserver,
                                                          final PublishSubject<HttpResponse> headerSubject,
                                                          final PublishSubject<T> contentSubject) {
        this.headerSubject = headerSubject;
        this.contentSubject = contentSubject;
        observableConnection.getInput().subscribe(new InputObserver<T>(headerSubject, contentSubject,
                                                                       requestProcessingObserver,
                                                                       observableConnection.channelContext()));
    }

    public Observable<T> content() {
        return contentSubject;
    }

    public Observable<HttpResponse> header() {
        return headerSubject;
    }

    private static class InputObserver<T> extends CompositeObserver<T> {

        private final PublishSubject<HttpResponse> headerSubject;
        private final PublishSubject<T> contentSubject;
        private final ChannelHandlerContext context;

        public InputObserver(PublishSubject<HttpResponse> headerSubject, PublishSubject<T> contentSubject,
                             Observer<? super ObservableHttpResponse<T>> requestProcessingObserver,
                             ChannelHandlerContext context) {
            super(headerSubject, contentSubject, requestProcessingObserver);
            this.headerSubject = headerSubject;
            this.contentSubject = contentSubject;
            this.context = context;
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            ReadTimeoutPipelineConfigurator.removeTimeoutHandler(context.pipeline());
        }

        @Override
        public void onNext(T msg) {
            Class<?> msgClass = msg.getClass();
            // See Class javadoc for detail about this behavior.
            if (FullHttpResponse.class.isAssignableFrom(msgClass)) {
                headerSubject.onNext((HttpResponse) msg);
                headerSubject.onCompleted();
                contentSubject.onNext(msg);
                onCompleted();
            } else if (HttpResponse.class.isAssignableFrom(msgClass)) {
                headerSubject.onNext((HttpResponse) msg);
                headerSubject.onCompleted();
            } else if (LastHttpContent.class.isAssignableFrom(msgClass)) {
                LastHttpContent lastHttpContent = (LastHttpContent) msg;
                if (lastHttpContent.content().isReadable()) { // Do not give callback for empty content.
                    contentSubject.onNext(msg);
                }
                onCompleted();
            } else if (HttpContent.class.isAssignableFrom(msgClass)) {
                contentSubject.onNext(msg);
            } else { // Custom object case.
                contentSubject.onNext(msg);
            }
        }
    }
}