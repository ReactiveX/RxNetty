package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.reactivex.netty.protocol.http.server.HttpServerRule.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpEndToEndTest {

    @Rule
    public final HttpServerRule rule = new HttpServerRule();

    @Test(timeout = 60000)
    public void testDelayedWrites() throws Exception {

        final AtomicReference<Throwable> errorFromWriteStreamCompletion = new AtomicReference<>();
        final Worker worker = Schedulers.computation().createWorker();
        rule.startServer(new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return response.writeString(Observable.create(new OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> subscriber) {
                        worker.schedule(new Action0() {
                            @Override
                            public void call() {
                                try {
                                    subscriber.onNext(WELCOME_SERVER_MSG);
                                    subscriber.onCompleted();
                                } catch (Exception e) {
                                    errorFromWriteStreamCompletion.set(e);
                                }
                            }
                        }, 1, TimeUnit.MILLISECONDS);

                    }
                }));
            }
        });

        final HttpClientResponse<ByteBuf> response = rule.sendRequest(rule.getClient().createGet("/"));

        assertThat("Unexpected response code.", response.getStatus(), is(HttpResponseStatus.OK));

        rule.assertResponseContent(response);

        assertThat("Unexpected exception on server.", errorFromWriteStreamCompletion.get(), is(nullValue()));
    }
}
