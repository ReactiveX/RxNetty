/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 * Modifications Copyright (c) 2017 RxNetty Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.netty.http.websocket;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.reactivestreams.Publisher;
import io.reactivex.netty.NettyOutbound;

/**
 * A websocket framed outbound
 *
 * @author Stephane Maldini
 * @author Simon Baslé
 * @since 0.6
 */
public interface WebsocketOutbound extends NettyOutbound {

	/**
	 * Returns the websocket subprotocol negotiated by the client and server during
	 * the websocket handshake, or null if none was requested.
	 *
	 * @return the subprotocol, or null
	 */
	String selectedSubprotocol();

	@Override
	default NettyOutbound send(Publisher<? extends ByteBuf> dataStream) {
		return sendObject(Flowable.fromPublisher(dataStream)
		                      .map(bytebufToWebsocketFrame));
	}

	@Override
	default NettyOutbound sendString(Publisher<? extends String> dataStream,
			Charset charset) {
		return sendObject(Flowable.fromPublisher(dataStream)
		                      .map(stringToWebsocketFrame));
	}

	Function<? super String, ? extends WebSocketFrame> stringToWebsocketFrame  =
			TextWebSocketFrame::new;
	Function<? super ByteBuf, ? extends WebSocketFrame> bytebufToWebsocketFrame =
			BinaryWebSocketFrame::new;
}
