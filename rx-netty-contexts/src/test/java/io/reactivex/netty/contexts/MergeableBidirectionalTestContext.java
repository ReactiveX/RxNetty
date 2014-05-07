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

package io.reactivex.netty.contexts;

import com.netflix.server.context.BiDirectional;
import com.netflix.server.context.MergeableContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nitesh Kant
 */
@BiDirectional
public class MergeableBidirectionalTestContext extends TestContext
        implements MergeableContext<MergeableBidirectionalTestContext> {

    private List<String> allNames = new ArrayList<String>();

    public MergeableBidirectionalTestContext(String name) {
        super(name);
    }

    @Override
    public void merge(MergeableBidirectionalTestContext toMerge) {
        allNames.addAll(toMerge.allNames);
    }

    public List<String> getAllNames() {
        return allNames;
    }

    public void addName(String name) {
        allNames.add(name);
    }
}
