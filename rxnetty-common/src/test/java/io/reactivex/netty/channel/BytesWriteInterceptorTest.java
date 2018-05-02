/*
 * Copyright 2016 Netflix, Inc.
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
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.AbstractScheduledEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.ObjectUtil;
import io.reactivex.netty.channel.BackpressureManagingHandler.BytesWriteInterceptor;
import io.reactivex.netty.channel.BackpressureManagingHandler.WriteStreamSubscriber;
import io.reactivex.netty.test.util.MockProducer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.channel.BackpressureManagingHandler.BytesWriteInterceptor.MAX_PER_SUBSCRIBER_REQUEST;
import static io.reactivex.netty.channel.BytesWriteInterceptorTest.InspectorRule.defaultRequestN;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static rx.Observable.just;

public class BytesWriteInterceptorTest {

    @Rule
    public final InspectorRule inspectorRule = new InspectorRule();

    @Test(timeout = 60000)
    public void testAddSubscriber() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();

        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), hasSize(1));
        assertThat("Subscriber not added.", inspectorRule.interceptor.getSubscribers(), contains(sub1));

        sub1.unsubscribe();
        inspectorRule.channel.runPendingTasks();
        assertThat("Subscriber not removed post unsubscribe", inspectorRule.interceptor.getSubscribers(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testRequestMore() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(defaultRequestN()));

        inspectorRule.sendMessages(1);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested.", mockProducer.getRequested(), is(defaultRequestN()));
    }

    @Test(timeout = 60000)
    public void testRequestMorePostFlush() throws Exception {

        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer mockProducer = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", mockProducer.getRequested(), is(defaultRequestN()));

        inspectorRule.channel.config().setWriteBufferWaterMark(new WriteBufferWaterMark(1, 2)); /*Make sure that the channel is not writable on writing.*/

        String msg = "Hello";
        inspectorRule.channel.write(msg);

        assertThat("Channel still writable.", inspectorRule.channel.isWritable(), is(false));
        assertThat("More items requested when channel is not writable.", mockProducer.getRequested(),
                   is(defaultRequestN()));

        inspectorRule.channel.flush();

        assertThat("Channel not writable post flush.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested.", mockProducer.getRequested(), is(defaultRequestN()));
    }

    @Test(timeout = 60000)
    public void testMultiSubscribers() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 1);

        WriteStreamSubscriber sub2 = inspectorRule.newSubscriber();
        MockProducer producer2 = inspectorRule.setupSubscriberAndValidate(sub2, 2);

        inspectorRule.sendMessages(1);

        assertThat("Channel not writable post write.", inspectorRule.channel.isWritable(), is(true));
        assertThat("Unexpected items requested from first subscriber.", producer1.getRequested(),
                   is(defaultRequestN()));
        assertThat("Unexpected items requested from second subscriber.", producer2.getRequested(),
                   is(defaultRequestN() / 2));
    }

    @Test(timeout = 10000)
    public void testOneLongWriteAndManySmallWrites() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", producer1.getRequested(), is(defaultRequestN()));
        inspectorRule.setupNewSubscriberAndComplete(2, true);
        inspectorRule.setupNewSubscriberAndComplete(2, true);

        inspectorRule.sendMessages(sub1, 33);
        assertThat("Unexpected items requested.", producer1.getRequested(), is(97L));
    }

    @Test(timeout = 100000)
    public void testWritesInOrderFromDifferentThreads() throws Exception {
        final WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();

        // Set the current thread to be the thread of the event loop
        inspectorRule.setEventLoopThread();

        // Send 1000 messages from two different threads
        int msgCount = 1000;
        Scheduler.Worker worker = Schedulers.computation().createWorker();
        for (int i = 1; i < msgCount; i+=2) {
            sub1.onNext(String.valueOf(i));

            // Send from other thread
            inspectorRule.sendFromOtherThread(sub1, worker, String.valueOf(i+1));
        }

        // In lack of a way of running all pending tasks on computation scheduler
        Thread.sleep(500);

        // Ensure messages are in order
        Queue<Object> written = inspectorRule.getWrittenMessages();
        for (int i = 1; i <= msgCount; i++) {
            Object msg = written.poll();
            String strMsg = ((ByteBuf) msg).toString(Charset.defaultCharset());
            assertThat("Not in order ", strMsg, is(String.valueOf(i)));
        }
    }

    @Test(timeout = 10000)
    public void testBatchedSubscriberRemoves() throws Exception {
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 1);
        assertThat("Unexpected items requested from producer.", producer1.getRequested(), is(defaultRequestN()));
        for (int i=1; i < 5; i++) {
            inspectorRule.setupNewSubscriberAndComplete(i+1, false);
        }

        inspectorRule.channel.runPendingTasks();

        inspectorRule.sendMessages(sub1, 35);
        assertThat("Unexpected items requested.", producer1.getRequested(), is(95L));
    }

    @Test(timeout = 10000)
    public void testMinRequestN() throws Exception {
        for (int i=1; i < 66; i++) {
            inspectorRule.setupNewSubscriberAndComplete(i, false);
        }
        WriteStreamSubscriber sub1 = inspectorRule.newSubscriber();
        MockProducer producer1 = inspectorRule.setupSubscriberAndValidate(sub1, 66);
        assertThat("Unexpected items requested from producer.", producer1.getRequested(), is(1L));

        inspectorRule.channel.runPendingTasks();
        inspectorRule.sendMessages(sub1, 35);
        assertThat("Unexpected items requested.", producer1.getRequested(), greaterThan(1L));
    }

    public static class InspectorRule extends ExternalResource {

        private BytesWriteInterceptor interceptor;
        private EmbeddedChannel channel;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    interceptor = new BytesWriteInterceptor("foo");
                    channel = new TestEmbeddedChannel(new WriteTransformer(), interceptor);
                    base.evaluate();
                }
            };
        }

        WriteStreamSubscriber newSubscriber() {
            return interceptor.newSubscriber(channel.pipeline().lastContext(), channel.newPromise());
        }

        private MockProducer setupSubscriberAndValidate(WriteStreamSubscriber sub, int expectedSubCount) {
            MockProducer mockProducer = setupSubscriber(sub);
            assertThat("Subscriber not added.", interceptor.getSubscribers(), hasSize(expectedSubCount));
            assertThat("Subscriber not added.", interceptor.getSubscribers().get(expectedSubCount - 1), equalTo(sub));
            return mockProducer;
        }

        private static MockProducer setupSubscriber(WriteStreamSubscriber sub) {
            sub.onStart();
            MockProducer mockProducer = new MockProducer();
            sub.setProducer(mockProducer);
            return mockProducer;
        }

        public static Long defaultRequestN() {
            return Long.valueOf(MAX_PER_SUBSCRIBER_REQUEST);
        }

        public void sendMessages(WriteStreamSubscriber subscriber, int msgCount) {
            for(int i=0; i < msgCount; i++) {
                subscriber.onNext("Hello");
                channel.write("Hello");
            }
            channel.flush();
        }

        public void sendMessages(int msgCount) {
            for(int i=0; i < msgCount; i++) {
                channel.write("Hello");
            }
            channel.flush();
        }

        public void setupNewSubscriberAndComplete(int expectedSubCount, boolean runPendingTasks) {
            WriteStreamSubscriber sub2 = newSubscriber();
            MockProducer producer2 = setupSubscriberAndValidate(sub2, expectedSubCount);
            assertThat("Unexpected items requested from producer.", producer2.getRequested(),
                       lessThanOrEqualTo(Math.max(1, defaultRequestN()/expectedSubCount)));
            sub2.onCompleted();
            sub2.unsubscribe();
            if (runPendingTasks) {
                channel.runPendingTasks();
            }
        }

        public Queue<Object> getWrittenMessages() {
            channel.runPendingTasks();
            channel.flush();
            return channel.outboundMessages();
        }

        public void setEventLoopThread() {
            ChannelPromise deregisterPromise = channel.newPromise();
            channel.deregister(deregisterPromise);
            channel.runPendingTasks();
            assertThat("failed to deregister", deregisterPromise.isDone() && deregisterPromise.isSuccess());

            ThreadAwareEmbeddedEventLoop loop = new ThreadAwareEmbeddedEventLoop(Thread.currentThread());
            ChannelFuture registerPromise = loop.register(channel);
            assertThat("failed to register", registerPromise.isDone() && registerPromise.isSuccess());
        }

        private void sendFromOtherThread(final WriteStreamSubscriber subscriber, Scheduler.Worker worker, final Object msg) throws InterruptedException {
            final CountDownLatch countDown = new CountDownLatch(1);
            worker.schedule(new Action0() {
                @Override
                public void call() {
                    subscriber.onNext(msg);
                    countDown.countDown();
                }
            });
            countDown.await();
        }
    }

    /**
     * A custom EmbeddedChannel allowing a special EventLoop, so that we can simulate calls not coming from the event loop.
     */
    private static class TestEmbeddedChannel extends EmbeddedChannel {

        public TestEmbeddedChannel(WriteTransformer writeTransformer, BytesWriteInterceptor interceptor) {
            super(writeTransformer, interceptor);
        }

        @Override
        protected boolean isCompatible(EventLoop loop) {
            return loop instanceof ThreadAwareEmbeddedEventLoop || super.isCompatible(loop);
        }

        @Override
        public void runPendingTasks() {
            if (super.eventLoop() instanceof ThreadAwareEmbeddedEventLoop) {
                ThreadAwareEmbeddedEventLoop loop = (ThreadAwareEmbeddedEventLoop) super.eventLoop();
                loop.runTasks();
            } else {
                super.runPendingTasks();
            }
        }
    }

    /**
     * Need an embedded event loop that considers a single thread to be "on the loop" in order to have writes from
     * outside the event loop.
     * Due to final modifier of EmbeddedEventLoop there was some copying needed.
     */
    private static class ThreadAwareEmbeddedEventLoop extends AbstractScheduledEventExecutor implements EventLoop {

        private final Queue<Runnable> tasks = new ArrayDeque<Runnable>(2);
        private final Thread loopThread;

        public ThreadAwareEmbeddedEventLoop(Thread loopThread) {
            this.loopThread = loopThread;
        }

        @Override
        public EventLoopGroup parent() {
            return (EventLoopGroup) super.parent();
        }

        @Override
        public EventLoop next() {
            return (EventLoop) super.next();
        }

        @Override
        public void execute(Runnable command) {
            if (command == null) {
                throw new NullPointerException("command");
            }
            tasks.add(command);
        }

        void runTasks() {
            for (;;) {
                Runnable task = tasks.poll();
                if (task == null) {
                    break;
                }

                task.run();
            }
        }

        @Override
        protected void cancelScheduledTasks() {
            super.cancelScheduledTasks();
        }

        @Override
        public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<?> terminationFuture() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShuttingDown() {
            return false;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public ChannelFuture register(Channel channel) {
            return register(new DefaultChannelPromise(channel, this));
        }

        @Override
        public ChannelFuture register(ChannelPromise promise) {
            ObjectUtil.checkNotNull(promise, "promise");
            promise.channel().unsafe().register(this, promise);
            return promise;
        }

        @Deprecated
        @Override
        public ChannelFuture register(Channel channel, ChannelPromise promise) {
            channel.unsafe().register(this, promise);
            return promise;
        }

        @Override
        public boolean inEventLoop() {
            return Thread.currentThread() == loopThread;
        }

        @Override
        public boolean inEventLoop(Thread thread) {
            return thread == loopThread;
        }
    }

}
