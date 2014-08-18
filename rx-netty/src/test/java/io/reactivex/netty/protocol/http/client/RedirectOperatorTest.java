/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nitesh Kant
 */
public class RedirectOperatorTest {

    @Test
    public void testMaxRedirects() throws Exception {

        Setup setup = new Setup().setup(HttpResponseStatus.TEMPORARY_REDIRECT);

        Assert.assertEquals("Unexpected redirect count.", 2, setup.getHandler().getRedirectsRequested());
        Assert.assertEquals("Unexpected onComplete calls to redirect subscriber.", 0, setup.getSubscriber().getOnCompletes());
        Assert.assertEquals("Unexpected onNext calls to redirect subscriber.", 0, setup.getSubscriber().getOnNexts());
        Assert.assertEquals("Unexpected onError calls to redirect subscriber.", 1, setup.getSubscriber().getOnErrors());
    }

    @Test
    public void testRedirect() throws Exception {

        Setup setup = new Setup().setup(HttpResponseStatus.OK);

        Assert.assertEquals("Unexpected redirect count.", 1, setup.getHandler().getRedirectsRequested());
        Assert.assertEquals("Unexpected onComplete calls to redirect subscriber.", 1, setup.getSubscriber().getOnCompletes());
        Assert.assertEquals("Unexpected onNext calls to redirect subscriber.", 1, setup.getSubscriber().getOnNexts());
        Assert.assertEquals("Unexpected onError calls to redirect subscriber.", 0, setup.getSubscriber().getOnErrors());
    }

    private static class TestableRedirectHandler<I, O> implements RedirectOperator.RedirectHandler<I, O> {

        private final HttpClientResponse<O> response;
        private final int maxHops;
        private final AtomicInteger redirectsRequested = new AtomicInteger();

        public TestableRedirectHandler(int maxHops, HttpResponseStatus redirectResponseStatus) {
            this.maxHops = maxHops;
            DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, redirectResponseStatus);
            response = new HttpClientResponse<O>(nettyResponse, UnicastContentSubject.<O>createWithoutNoSubscriptionTimeout());
        }

        public TestableRedirectHandler(int maxHops) {
            this(maxHops, HttpResponseStatus.TEMPORARY_REDIRECT);
        }

        @Override
        public Observable<HttpClientResponse<O>> doRedirect(RedirectionContext context,
                                                            HttpClientRequest<I> originalRequest,
                                                            HttpClient.HttpClientConfig config) {
            redirectsRequested.incrementAndGet();
            return Observable.just(response);
        }

        @Override
        public boolean requiresRedirect(RedirectionContext context, HttpClientResponse<O> response) {
            return response.getStatus() == HttpResponseStatus.TEMPORARY_REDIRECT;
        }

        @Override
        public void validate(RedirectionContext context, HttpClientResponse<O> redirectResponse) {
            if(context.getRedirectCount() >= maxHops) {
                throw new HttpRedirectException(HttpRedirectException.Reason.TooManyRedirects, "");
            }
        }

        private int getRedirectsRequested() {
            return redirectsRequested.get();
        }
    }

    private static class UnsafeRedirectSubscriber extends Subscriber<HttpClientResponse<ByteBuf>> {
        private final AtomicInteger onCompletes;
        private final CountDownLatch completeLatch;
        private final AtomicInteger onErrors;
        private final AtomicInteger onNexts;

        public UnsafeRedirectSubscriber() {
            onCompletes = new AtomicInteger();
            completeLatch = new CountDownLatch(1);
            onErrors = new AtomicInteger();
            onNexts = new AtomicInteger();
        }

        @Override
        public void onCompleted() {
            onCompletes.incrementAndGet();
            completeLatch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            onErrors.incrementAndGet();
            completeLatch.countDown();
        }

        @Override
        public void onNext(HttpClientResponse<ByteBuf> response) {
            onNexts.incrementAndGet();
        }

        public int getOnCompletes() {
            return onCompletes.get();
        }

        public int getOnErrors() {
            return onErrors.get();
        }

        public int getOnNexts() {
            return onNexts.get();
        }

        public void waitForCompletion(int time, TimeUnit unit) throws InterruptedException {
            completeLatch.await(time, unit);
        }
    }

    private static class Setup {

        private TestableRedirectHandler<ByteBuf, ByteBuf> handler;
        private UnsafeRedirectSubscriber subscriber;

        public TestableRedirectHandler<ByteBuf, ByteBuf> getHandler() {
            return handler;
        }

        public UnsafeRedirectSubscriber getSubscriber() {
            return subscriber;
        }

        public Setup setup(HttpResponseStatus redirectStatus) throws InterruptedException {
            DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "");
            HttpClientRequest<ByteBuf> request = new HttpClientRequest<ByteBuf>(nettyRequest);
            DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                                                        HttpResponseStatus.TEMPORARY_REDIRECT);
            final HttpClientResponse<ByteBuf> response =
                    new HttpClientResponse<ByteBuf>(nettyResponse,
                                              UnicastContentSubject .<ByteBuf>createWithoutNoSubscriptionTimeout());
            handler = new TestableRedirectHandler<ByteBuf, ByteBuf>(2, redirectStatus);

            subscriber = new UnsafeRedirectSubscriber();
            Observable.just(response)
                      .lift(new RedirectOperator<ByteBuf, ByteBuf>(request, handler))
                      .unsafeSubscribe(subscriber);

            subscriber.waitForCompletion(1, TimeUnit.MINUTES);
            return this;
        }
    }
}
