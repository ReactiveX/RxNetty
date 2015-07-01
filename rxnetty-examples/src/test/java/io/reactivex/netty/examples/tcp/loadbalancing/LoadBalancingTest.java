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

package io.reactivex.netty.examples.tcp.loadbalancing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static io.reactivex.netty.examples.ExamplesTestUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class LoadBalancingTest {

    @Test(timeout = 60000)
    public void testLoadBalancing() throws Exception {
        final Queue<String> output = setupClientLogger(TcpLoadBalancingClient.class);

        TcpLoadBalancingClient.main(null);

        assertThat("Unexpected number of messages echoed", output, hasSize(10));

        Map<String, Integer> hostsVsCount = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            String hostUsed = output.poll();
            Integer existingCount = hostsVsCount.get(hostUsed);
            if (null == existingCount) {
                hostsVsCount.put(hostUsed, 1);
            } else {
                hostsVsCount.put(hostUsed, existingCount.intValue() + 1);
            }
            assertThat("Unexpected content.", hostUsed, startsWith("Using host:"));
            assertThat("Unexpected content.", output.poll(), equalTo("Hello World!"));
        }

        assertThat("Unexpected number of hosts used.", hostsVsCount.entrySet(), hasSize(2));
    }
}