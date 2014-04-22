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

package com.netflix.server.context;

import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.ContextsContainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any contexts added to {@link ContextsContainer#addContext(String, Object, ContextSerializer)} is unidirectional i.e.
 * it always flows down the call chain, eg: In the below call graph
 <PRE>
        Service A ---> Service B ---> Service C
                                |
                                 ---> Service D

 ------------------ Context X Flows through the call graph ----------------->
 </PRE>

 * Any changes to Context "X" (which gets added by Service A) in Service B will only be available to Service C & D but not
 * service A. <br/>
 * This interface addresses the use-case where the update in context X must be available to Service A. <br/>
 *
 * By annotating their class with this annotation, the context implementers can guarantee that the context changes,
 * flow bi-directionally in the call graph i.e. any code invoked after the change will get the updated context. <br/>
 * These updates will be effective iff, the following steps are taken:
 * <ul>
 <li>Whenever the context gets updated, update the same with {@link ContextsContainer} by calling
 {@link ContextsContainer#addContext(String, Object, ContextSerializer)}. Implementation may batch these updates if such a
 batch can be made reliably.</li>
 <li>Before returning a response from the service, get the updated bidirectional contexts via
 {@link ContextsContainer#getModifiedBidirectionalContexts()} and add it as response headers.</li>
 <li>On receiving a response, read & update bidirectional contexts by calling
 {@link ContextsContainer#consumeBidirectionalContextsFromResponse(ContextKeySupplier)} </li>
 </ul>
 *
 * NIWS infrastructure does the last two steps transparently. If you (or your dependencies) are not using NIWS you may
 * have to do either one or both of these yourselves based on whether you are using client side, server side or none
 * of the NIWS components.
 *
 * <h2>Mergeable contexts</h2>
 * The infrastructure always over-writes the existing context when writing back a bi-directional context to the current
 * thread. In the above example, when service B returns, it reads back the bi-directional context & update the context
 * in Service A, at this point the default behavior is to over-write the existing context. However, in some cases, it
 * may be desirable to merge the two contexts. If so, is the case with your context, you must implement the interface
 * {@link MergeableContext} to indicate that the old context must be merged into the new context. <br/>
 * For such a context, the infrastructure will call {@link MergeableContext#merge(MergeableContext)} on the old context
 * with the new context as the argument.
 *
 * <h2>New Contexts added in Child service</h2>
 * Any bi-directional contexts added fresh in a child service also flows back in the response.
 *
 * @author Nitesh Kant
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BiDirectional {
}
