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

package io.reactivex.netty.examples.http.pipelining;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.examples.ExamplesEnvironment;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Queue;

import static io.reactivex.netty.examples.ExamplesTestUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PipeliningTest extends ExamplesEnvironment {

    @Test(timeout = 60000)
    public void testPipelining() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Queue<String> output = setupClientLogger(PipeliningClient.class);

        PipeliningClient.main(null);

        assertThat("Unexpected number of messages echoed", output, hasSize(2));

        assertThat("Unexpected response.", output, contains(HttpResponseStatus.OK.toString(),
                                                            HttpResponseStatus.OK.toString())
        );
    }
}
