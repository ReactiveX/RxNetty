package rx.netty.experimental.protocol.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;

/**
 * This class is responsible for sending a request object to the server and then setting up
 * proper listeners when sending request is successful
 *
 * @param <T> The type of response content
 * @param <R> The type of the request
 */
class RequestWriter<T, R extends HttpRequest> {
    private final Channel channel;

    RequestWriter(Channel channel) {
        this.channel = channel;
    }

    Future<T> execute(R request, final RequestCompletionPromise requestCompletionPromise) {
        ChannelPromise promise = channel.newPromise();
        channel.writeAndFlush(request, promise).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    requestCompletionPromise.tryFailure(future.cause());
                }
            }
        });

        return new RequestWrittenPromise(channel, promise);
    }

    Channel getChannel() {
        return channel;
    }

    public boolean isActive() {
        return channel.isActive();
    }

    private class RequestWrittenPromise extends DefaultPromise<T> {

        private final ChannelPromise sendRequestPromise;

        public RequestWrittenPromise(Channel channel, ChannelPromise sendRequestPromise) {
            super(channel.eventLoop());
            this.sendRequestPromise = sendRequestPromise;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (sendRequestPromise.isCancellable()) {
                sendRequestPromise.cancel(mayInterruptIfRunning);
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }
}