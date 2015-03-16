/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http.serverNew;

import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.internal.OperatorTrailer;
import io.reactivex.netty.protocol.http.serverNew.HttpServerResponse.ContentWriter;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

final class ContentWriterImpl<C> extends ContentWriter<C> {

    @SuppressWarnings("rawtypes")
    private final Connection connection;

    @SuppressWarnings("rawtypes")
    private final Observable headersObservable;
    @SuppressWarnings("rawtypes")
    private final Observable contentObservable;

    private final HttpResponse headers;

    private final Func1<C, Boolean> flushOnEachSelector = new Func1<C, Boolean>() {
        @Override
        public Boolean call(C w) {
            return true;
        }
    };

    ContentWriterImpl(@SuppressWarnings("rawtypes") final Connection connection, final HttpResponse headers) {
        super(new OnSubscribe<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                /*We are never sending content as the subscription is to the headers only writer.*/
                headers.headers().set(Names.CONTENT_LENGTH, 0);
                connection.writeAndFlush(headers).subscribe(subscriber);
            }
        });
        this.connection = connection;
        this.headers = headers;
        headersObservable = Observable.just(headers);
        contentObservable = null;
    }

    private ContentWriterImpl(final ContentWriterImpl<C> parent,
                              @SuppressWarnings("rawtypes") final Observable content, final boolean appendTrailer) {
        super(new OnSubscribe<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                setTransferEncodingIfNoContentLength(parent.headers);
                parent.connection.writeAndFlush(getHttpStream(parent, content, appendTrailer))
                                 .subscribe(subscriber);
            }
        });
        connection = parent.connection;
        headers = parent.headers;
        headersObservable = parent.headersObservable;
        if (null == parent.contentObservable) {
            contentObservable = content;
        } else {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Observable rawMerged = parent.contentObservable.mergeWith(content);
            contentObservable = rawMerged;
        }
    }

    @Override
    public ContentWriter<C> write(Observable<C> msgs) {
        return new ContentWriterImpl<>(this, msgs, true);
    }

    @Override
    public <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource,
                                                              Func0<T> trailerFactory,
                                                              Func2<T, C, T> trailerMutator) {
        @SuppressWarnings("rawtypes")
        Observable rawObservable = contentSource;
        return new ContentWriterImpl<>(this, OperatorTrailer.liftFrom(rawObservable, trailerFactory, trailerMutator),
                                       false);
    }

    @Override
    public ContentWriter<C> write(Observable<C> msgs, final Func1<C, Boolean> flushSelector) {
        return new ContentWriterImpl<>(this, msgs.map(new Func1<C, C>() {
            @Override
            public C call(C nextItem) {
                if (flushSelector.call(nextItem)) {
                    connection.getNettyChannel().flush();
                }
                return nextItem;
            }
        }), true);
    }

    @Override
    public ContentWriter<C> writeAndFlushOnEach(Observable<C> msgs) {
        return write(msgs, flushOnEachSelector);
    }

    private static void setTransferEncodingIfNoContentLength(HttpResponse headers) {
        if (!headers.headers().contains(Names.CONTENT_LENGTH)) {
            // If there is no content length we need to specify the transfer encoding as chunked as we always
            // send data in multiple HttpContent.
            // On the other hand, if someone wants to not have chunked encoding, adding content-length will work
            // as expected.
            headers.headers().set(Names.TRANSFER_ENCODING, Values.CHUNKED);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Observable getHttpStream(ContentWriterImpl parent, Observable content, boolean appendTrailer) {
        Observable httpStream = parent.headersObservable;
        if (null != parent.contentObservable) {
            httpStream = httpStream.concatWith(parent.contentObservable.mergeWith(content));
        } else {
            httpStream = httpStream.concatWith(content);
        }

        if (appendTrailer) {
            httpStream = httpStream.concatWith(Observable.just(new DefaultLastHttpContent()));
        }

        return httpStream;
    }
}
