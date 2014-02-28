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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import org.junit.Test;

public class RepeatableRequestTest {
    @Test
    public void testRepeatableRequest() {
        final List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }
        
        ContentSource<Integer> source = new ContentSource<Integer>() {
            Iterator<Integer> iterator = list.iterator();
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Integer next() {
                return iterator.next();
            }
        };
        
        HttpRequest<Integer> request = HttpRequest.<Integer>create(HttpVersion.HTTP_1_1, HttpMethod.POST, "/")
                .withContentSource(source);
        HttpRequest<Integer> repeatable = new RepeatableContentHttpRequest<Integer>(request);
        ContentFactory<Integer, ContentSource<Integer>> factory = (ContentFactory<Integer, ContentSource<Integer>>) repeatable.contentFactory;
        ContentSource<Integer> source2 = factory.newContentSource();
        source2.next();
        source2.next();
        source2 = factory.newContentSource();
        List<Integer> result = new ArrayList<Integer>();
        while (source2.hasNext()) {
            result.add(source2.next());
        }
        assertEquals(list, result);

        // go over again
        source2 = factory.newContentSource();
        result = new ArrayList<Integer>();
        while (source2.hasNext()) {
            result.add(source2.next());
        }
        assertEquals(list, result);
    }

}
