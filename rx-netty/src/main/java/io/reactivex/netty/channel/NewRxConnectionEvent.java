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
package io.reactivex.netty.channel;

import rx.Observer;

/**
 * Fired whenever a new {@link ObservableConnection} instance is created.
 *
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public class NewRxConnectionEvent extends AbstractConnectionEvent<ObservableConnection> {

    public NewRxConnectionEvent(final ObservableConnection<?, ?> observableConnection,
                                final Observer connectedObserver) {
        super(connectedObserver, observableConnection);
    }

}
