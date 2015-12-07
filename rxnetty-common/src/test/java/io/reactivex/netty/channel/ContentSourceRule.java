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
 *
 */

package io.reactivex.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

public class ContentSourceRule extends ExternalResource implements Func1<Subscriber<? super ByteBuf>, Object> {

    private ByteBuf data;
    private EmbeddedChannel channel;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                data = Unpooled.buffer().writeBytes("Hello".getBytes());
                channel = new EmbeddedChannel(new ChannelDuplexHandler() {
                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        if (evt instanceof SourceEvent) {
                            SourceEvent sourceEvent = (SourceEvent) evt;
                            sourceEvent.subscriber.onNext(data);
                            sourceEvent.subscriber.onCompleted();
                        }
                        super.userEventTriggered(ctx, evt);
                    }
                });
                base.evaluate();
            }
        };
    }

    public EmbeddedChannel getChannel() {
        return channel;
    }

    public ByteBuf getData() {
        return data;
    }

    public TestSubscriber<ByteBuf> subscribe(Observable<ByteBuf> source, int expectedRefCnt) {
        TestSubscriber<ByteBuf> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertValue(data);

        ByteBuf data = subscriber.getOnNextEvents().get(0);

        assertThat("Unexpected ref count of data", data.refCnt(), is(expectedRefCnt));
        return subscriber;
    }

    @Override
    public Object call(Subscriber<? super ByteBuf> subscriber) {
        return new SourceEvent(subscriber);
    }

    public static class SourceEvent {

        private final Subscriber<? super ByteBuf> subscriber;

        public SourceEvent(Subscriber<? super ByteBuf> subscriber) {
            this.subscriber = subscriber;
        }

        public Subscriber<?> getSubscriber() {
            return subscriber;
        }
    }
}
