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
package io.reactivex.netty.protocol.tcp.client;

import java.net.SocketAddress;

/**
 * A factory to create {@link ConnectionFactory} for different {@link SocketAddress}.
 *
 * @param <W> Type of object that is written to the connections created by this factory.
 * @param <R> Type of object that is read from the connections created by this factory.
 */
public abstract class BootstrapFactory<W, R> {

    public abstract ConnectionFactory<W, R> forHost(SocketAddress hostAddress);

}
