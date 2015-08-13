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
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.nio.charset.Charset;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PrimitiveConversionHandlerTest {

    @Rule
    public final ConverterRule converterRule = new ConverterRule();

    @Test(timeout = 60000)
    public void testWriteString() throws Exception {
        String msg = "Hello";
        converterRule.channel.writeAndFlush(msg);
        assertThat("String not written", converterRule.channel.outboundMessages(), hasSize(1));
        Object writtenMsg = converterRule.channel.readOutbound();
        assertThat("String not written as buffer", writtenMsg, is(instanceOf(ByteBuf.class)));

        ByteBuf asBB = (ByteBuf) writtenMsg;

        assertThat("Unexpected content of buffer written.", asBB.toString(Charset.defaultCharset()), equalTo(msg));
    }

    @Test(timeout = 60000)
    public void testWriteByteArray() throws Exception {
        byte[] msg = "Hello".getBytes();
        converterRule.channel.writeAndFlush(msg);
        assertThat("Bytes not written", converterRule.channel.outboundMessages(), hasSize(1));
        Object writtenMsg = converterRule.channel.readOutbound();
        assertThat("Bytes not written as buffer", writtenMsg, is(instanceOf(ByteBuf.class)));

        byte[] asBytes = new byte[msg.length];
        ((ByteBuf) writtenMsg).readBytes(asBytes);

        assertThat("Unexpected content of buffer written.", asBytes, equalTo(msg));
    }


    public static class ConverterRule extends ExternalResource {

        private PrimitiveConversionHandler converter;
        private EmbeddedChannel channel;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    converter = new PrimitiveConversionHandler();
                    channel = new EmbeddedChannel(converter);
                    base.evaluate();
                }
            };
        }
    }

}