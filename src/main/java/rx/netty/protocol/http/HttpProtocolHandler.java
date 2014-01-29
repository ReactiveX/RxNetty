/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.netty.protocol.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpResponse;
import rx.netty.protocol.tcp.ProtocolHandler;


/**
 *
 */
public interface HttpProtocolHandler<T> extends ProtocolHandler<Void, T> {

    public static final HttpProtocolHandler<Message> SSE_HANDLER = new HttpProtocolHandlerAdapter<Message>() {
        @Override
        public void configure(ChannelPipeline pipeline) {
            pipeline.addAfter("http-response-decoder", SSEHandler.NAME, new SSEHandler());
        }        
    };

    public static final HttpProtocolHandler<FullHttpResponse> FULL_HTTP_RESPONSE_HANDLER = new FullHttpResponseHandler();
    
    public void configure(ChannelPipeline pipeline);
    
    public void onChannelConnectOperationCompleted(ChannelFuture connectFuture);
    
    public void onChannelWriteOperationCompleted(ChannelFuture requestWrittenFuture);
    
    public void onChannelCloseOperationCompleted(ChannelFuture channelCloseFuture);
}
