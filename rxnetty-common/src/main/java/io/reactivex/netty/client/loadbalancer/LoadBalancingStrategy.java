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

package io.reactivex.netty.client.loadbalancer;

import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.HostConnector;

import java.util.List;

public interface LoadBalancingStrategy<W, R> {

    ConnectionProvider<W, R> newStrategy(List<HostHolder<W, R>> hosts);

    HostHolder<W, R> toHolder(HostConnector<W, R> connector);

}
