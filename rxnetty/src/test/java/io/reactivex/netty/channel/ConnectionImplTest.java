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
package io.reactivex.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.FileRegion;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.test.util.FlushSelector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import rx.Observable;

public class ConnectionImplTest {

    @Rule
    public final ConnRule connRule = new ConnRule();

    @Test(timeout = 60000)
    public void testWrite() throws Exception {

        Observable<ByteBuf> toWrite = Observable.empty();
        connRule.connection.write(toWrite);

        Mockito.verify(connRule.channelOperations).write(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteWithFlushSelector() throws Exception {
        Observable<ByteBuf> toWrite = Observable.empty();
        FlushSelector<ByteBuf> flushSelector = new FlushSelector<>(1);
        connRule.connection.write(toWrite, flushSelector);

        Mockito.verify(connRule.channelOperations).write(toWrite, flushSelector);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteAndFlushOnEach() throws Exception {
        Observable<ByteBuf> toWrite = Observable.empty();
        connRule.connection.writeAndFlushOnEach(toWrite);

        Mockito.verify(connRule.channelOperations).writeAndFlushOnEach(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteString() throws Exception {
        Observable<String> toWrite = Observable.empty();
        connRule.connection.writeString(toWrite);

        Mockito.verify(connRule.channelOperations).writeString(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);

    }

    @Test(timeout = 60000)
    public void testWriteStringWithFlushSelector() throws Exception {
        Observable<String> toWrite = Observable.empty();
        FlushSelector<String> flushSelector = new FlushSelector<>(1);
        connRule.connection.writeString(toWrite, flushSelector);

        Mockito.verify(connRule.channelOperations).writeString(toWrite, flushSelector);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteStringAndFlushOnEach() throws Exception {
        Observable<String> toWrite = Observable.empty();
        connRule.connection.writeStringAndFlushOnEach(toWrite);

        Mockito.verify(connRule.channelOperations).writeStringAndFlushOnEach(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteBytes() throws Exception {
        Observable<byte[]> toWrite = Observable.empty();
        connRule.connection.writeBytes(toWrite);

        Mockito.verify(connRule.channelOperations).writeBytes(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);

    }

    @Test(timeout = 60000)
    public void testWriteBytesWithFlushSelector() throws Exception {
        Observable<byte[]> toWrite = Observable.empty();
        FlushSelector<byte[]> flushSelector = new FlushSelector<>(1);
        connRule.connection.writeBytes(toWrite, flushSelector);

        Mockito.verify(connRule.channelOperations).writeBytes(toWrite, flushSelector);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteBytesAndFlushOnEach() throws Exception {
        Observable<byte[]> toWrite = Observable.empty();
        connRule.connection.writeBytesAndFlushOnEach(toWrite);

        Mockito.verify(connRule.channelOperations).writeBytesAndFlushOnEach(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteFileRegion() throws Exception {
        Observable<FileRegion> toWrite = Observable.empty();
        connRule.connection.writeFileRegion(toWrite);

        Mockito.verify(connRule.channelOperations).writeFileRegion(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);

    }

    @Test(timeout = 60000)
    public void testWriteFileRegionWithFlushSelector() throws Exception {
        Observable<FileRegion> toWrite = Observable.empty();
        FlushSelector<FileRegion> flushSelector = new FlushSelector<>(1);
        connRule.connection.writeFileRegion(toWrite, flushSelector);

        Mockito.verify(connRule.channelOperations).writeFileRegion(toWrite, flushSelector);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testWriteFileRegionAndFlushOnEach() throws Exception {
        Observable<FileRegion> toWrite = Observable.empty();
        connRule.connection.writeFileRegionAndFlushOnEach(toWrite);

        Mockito.verify(connRule.channelOperations).writeFileRegionAndFlushOnEach(toWrite);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testFlush() throws Exception {
        connRule.connection.flush();

        Mockito.verify(connRule.channelOperations).flush();
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testClose() throws Exception {
        connRule.connection.close();

        Mockito.verify(connRule.channelOperations).close();
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    @Test(timeout = 60000)
    public void testCloseWithoutFlush() throws Exception {
        connRule.connection.close(false);

        Mockito.verify(connRule.channelOperations).close(false);
        Mockito.verifyNoMoreInteractions(connRule.channelOperations);
    }

    public static class ConnRule extends ExternalResource {

        private ChannelOperations<ByteBuf> channelOperations;
        private Connection<ByteBuf, ByteBuf> connection;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final EmbeddedChannel channel = new EmbeddedChannel();

                    @SuppressWarnings("unchecked")
                    ChannelOperations<ByteBuf> channelOperations = Mockito.mock(ChannelOperations.class);

                    ConnRule.this.channelOperations = channelOperations;
                    connection = ConnectionImpl.create(channel, new MetricEventsSubject<>(),
                                                       ClientChannelMetricEventProvider.INSTANCE,
                                                       ConnRule.this.channelOperations);
                    base.evaluate();
                }
            };
        }
    }
}