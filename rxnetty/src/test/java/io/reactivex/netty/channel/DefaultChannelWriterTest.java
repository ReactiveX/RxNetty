package io.reactivex.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PrimitiveConversionHandler;
import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class DefaultChannelWriterTest {

    @Rule
    public final WriterRule writerRule = new WriterRule();

    @Test
    public void testWriteAndFlush() throws Exception {
        writerRule.writer.writeAndFlush(1);
        writerRule.assertMessagesWritten(1);
    }

    @Test
    public void testWriteAndFlushWithTransformer() throws Exception {
        final ByteBuf toWriteAsBuffer = Unpooled.buffer().writeBytes("1".getBytes());
        final Integer intToWrite = 1;
        final AtomicReference<Integer> dataToTransformer = new AtomicReference<Integer>();
        writerRule.writer.writeAndFlush(intToWrite, new ContentTransformer<Integer>() {
            @Override
            public ByteBuf call(Integer integer, ByteBufAllocator allocator) {
                dataToTransformer.set(integer);
                return toWriteAsBuffer;
            }
        });

        writerRule.assertMessagesWritten(toWriteAsBuffer);
        Assert.assertEquals("Unexpected message sent to tranformer.", intToWrite, dataToTransformer.get());
    }

    @Test
    public void testWriteBytesAndFlush() throws Exception {
        byte[] toWrite = new byte[1];
        toWrite[0] = 1;
        writerRule.writer.writeBytesAndFlush(toWrite);
        writerRule.assertMessagesWritten(Unpooled.buffer().writeBytes(toWrite));
    }

    @Test
    public void testWrite() throws Exception {
        writerRule.writer.write(1);
        writerRule.embeddedChannel.flush();
        writerRule.assertMessagesWritten(1);
    }

    @Test
    public void testWriteWithTransformer() throws Exception {
        final ByteBuf toWriteAsBuffer = Unpooled.buffer().writeBytes("1".getBytes());
        final Integer intToWrite = 1;
        final AtomicReference<Integer> dataToTransformer = new AtomicReference<Integer>();
        writerRule.writer.writeAndFlush(intToWrite, new ContentTransformer<Integer>() {
            @Override
            public ByteBuf call(Integer integer, ByteBufAllocator allocator) {
                dataToTransformer.set(integer);
                return toWriteAsBuffer;
            }
        });

        writerRule.embeddedChannel.flush();

        writerRule.assertMessagesWritten(toWriteAsBuffer);
        Assert.assertEquals("Unexpected message sent to tranformer.", intToWrite, dataToTransformer.get());
    }

    @Test
    public void testWriteByteBuf() throws Exception {
        ByteBuf toWrite = Unpooled.buffer().writeBytes("Hello".getBytes());
        writerRule.writer.writeBytes(toWrite);

        writerRule.embeddedChannel.flush();

        writerRule.assertMessagesWritten(toWrite);
    }

    @Test
    public void testWriteByteArray() throws Exception {
        byte[] toWrite = new byte[1];
        toWrite[0] = 1;
        writerRule.writer.writeBytes(toWrite);

        writerRule.embeddedChannel.flush();

        writerRule.assertMessagesWritten(Unpooled.buffer().writeBytes(toWrite));
    }

    @Test
    public void testWriteString() throws Exception {
        String toWriteStr = "Hello";
        writerRule.writer.writeString(toWriteStr);

        writerRule.embeddedChannel.flush();

        writerRule.assertMessagesWritten(Unpooled.buffer().writeBytes(toWriteStr.getBytes()));
    }

    @Test
    public void testWriteByteBufAndFlush() throws Exception {
        ByteBuf toWrite = Unpooled.buffer().writeBytes("Hello".getBytes());
        writerRule.writer.writeBytesAndFlush(toWrite);
        writerRule.assertMessagesWritten(toWrite);
    }

    @Test
    public void testWriteStringAndFlush() throws Exception {
        String toWriteStr = "Hello";
        writerRule.writer.writeStringAndFlush(toWriteStr);
        writerRule.assertMessagesWritten(Unpooled.buffer().writeBytes(toWriteStr.getBytes()));

    }

    public static class WriterRule extends ExternalResource {

        private DefaultChannelWriter<Integer> writer;
        private EmbeddedChannel embeddedChannel;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    embeddedChannel = new EmbeddedChannel(new PrimitiveConversionHandler());
                    writer = new DefaultChannelWriter<Integer>(embeddedChannel,
                                                               new MetricEventsSubject<ClientMetricsEvent<?>>(),
                                                               ClientChannelMetricEventProvider.INSTANCE);
                    base.evaluate();
                }
            };
        }

        public void assertMessagesWritten(Object... msgs) {
            Queue<Object> msgsWritten = embeddedChannel.outboundMessages();

            assertNotNull("Message(s) not written on the channel.", msgsWritten);

            assertEquals("Unexpected number of messages written on the channel.", msgs.length, msgsWritten.size());

            for (Object msg : msgs) {
                Object msgWritten = msgsWritten.poll();
                assertEquals("Unexpected message written on the channel.", msg, msgWritten);
            }
        }
    }

}