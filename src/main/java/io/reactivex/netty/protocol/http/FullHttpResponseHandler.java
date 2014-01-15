package io.reactivex.netty.protocol.http;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class FullHttpResponseHandler extends HttpProtocolHandlerAdapter<FullHttpResponse> {
    private final int timeout;
    
    public FullHttpResponseHandler() {
        timeout = 0;        
    }
    
    public FullHttpResponseHandler(int timeoutMillis) {
        this.timeout = timeoutMillis;        
    }
    
    @Override
    public void configure(ChannelPipeline pipeline) {
        pipeline.addAfter("http-response-decoder", "http-aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
    }

    
    @Override
    public void onChannelWriteOperationCompleted(ChannelFuture requestWrittenFuture) {
        if (timeout <= 0 ) {
            return;
        }
        if (requestWrittenFuture.isSuccess()) {
            ChannelPipeline pipeline = requestWrittenFuture.channel().pipeline();
            pipeline.addAfter("http-aggregator", SelfRemovingResponseTimeoutHandler.NAME, new SelfRemovingResponseTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
        }
    }
}
