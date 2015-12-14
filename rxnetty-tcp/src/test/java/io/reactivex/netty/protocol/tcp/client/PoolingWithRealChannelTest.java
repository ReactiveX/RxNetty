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
package io.reactivex.netty.protocol.tcp.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.client.pool.PooledConnection;
import org.junit.Rule;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * This tests the code paths which are not invoked for {@link EmbeddedChannel} as it does not schedule any task
 * (an EmbeddedChannelEventLopp never returns false for isInEventLoop())
 */
public class PoolingWithRealChannelTest {

    @Rule
    public final TcpClientRule clientRule = new TcpClientRule();

    /**
     * This test validates the async onNext and synchronous onComplete/onError nature of pooling when the connection is
     * reused.
     */
    //@Test(timeout = 60000)
    public void testReuse() throws Exception {
        clientRule.startServer(1);
        PooledConnection<ByteBuf, ByteBuf> connection = clientRule.connect();
        connection.closeNow();

        assertThat("Pooled connection is closed.", connection.unsafeNettyChannel().isOpen(), is(true));

        PooledConnection<ByteBuf, ByteBuf> connection2 = clientRule.connect();

        assertThat("Connection is not reused.", connection2, is(connection));
    }
}
