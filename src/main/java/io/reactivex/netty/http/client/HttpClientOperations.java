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

package io.reactivex.netty.http.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.netty.*;
import io.reactivex.netty.channel.ContextHandler;
import io.reactivex.netty.http.HttpOperations;
import io.reactivex.netty.http.websocket.WebsocketInbound;
import io.reactivex.netty.http.websocket.WebsocketOutbound;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import io.reactivex.netty.http.Cookies;

/**
 * @author Stephane Maldini
 * @author Simon Baslé
 */
class HttpClientOperations extends HttpOperations<HttpClientResponse, HttpClientRequest>
		implements HttpClientResponse, HttpClientRequest {

	static HttpOperations bindHttp(Channel channel,
			BiFunction<? super HttpClientResponse, ? super HttpClientRequest, ? extends Publisher<Void>> handler,
			ContextHandler<?> context) {
		return new HttpClientOperations(channel, handler, context);
	}

	final String[]    redirectedFrom;
	final boolean     isSecure;
	final HttpRequest nettyRequest;
	final HttpHeaders requestHeaders;

	volatile ResponseState responseState;
	int inboundPrefetch;

	boolean started;

	boolean clientError = true;
	boolean serverError = true;
	boolean redirectable;

	HttpClientOperations(Channel channel, HttpClientOperations replaced) {
		super(channel, replaced);
		this.started = replaced.started;
		this.redirectedFrom = replaced.redirectedFrom;
		this.isSecure = replaced.isSecure;
		this.nettyRequest = replaced.nettyRequest;
		this.responseState = replaced.responseState;
		this.redirectable = replaced.redirectable;
		this.inboundPrefetch = replaced.inboundPrefetch;
		this.requestHeaders = replaced.requestHeaders;
		this.clientError = replaced.clientError;
		this.serverError = replaced.serverError;
	}

	HttpClientOperations(Channel channel,
			BiFunction<? super HttpClientResponse, ? super HttpClientRequest, ? extends Publisher<Void>> handler,
			ContextHandler<?> context) {
		super(channel, handler, context);
		this.isSecure = channel.pipeline()
		                       .get(NettyPipeline.SslHandler) != null;
		String[] redirects = channel.attr(REDIRECT_ATTR_KEY)
		                            .get();
		this.redirectedFrom = redirects == null ? EMPTY_REDIRECTIONS : redirects;
		this.nettyRequest =
				new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
		this.requestHeaders = nettyRequest.headers();
		this.requestHeaders.set(HttpHeaderNames.USER_AGENT, HttpClient.USER_AGENT);
		this.inboundPrefetch = 16;
		chunkedTransfer(true);
	}

	@Override
	public HttpClientRequest addCookie(Cookie cookie) {
		if (!hasSentHeaders()) {
			this.requestHeaders.add(HttpHeaderNames.COOKIE,
					ClientCookieEncoder.STRICT.encode(cookie));
		}
		else {
			throw new IllegalStateException("Status and headers already sent");
		}
		return this;
	}

	@Override
	public HttpClientOperations addHandlerLast(ChannelHandler handler) {
		super.addHandlerLast(handler);
		return this;
	}

	@Override
	public HttpClientOperations addHandlerLast(String name, ChannelHandler handler) {
		super.addHandlerLast(name, handler);
		return this;
	}

	@Override
	public HttpClientOperations addHandlerFirst(ChannelHandler handler) {
		super.addHandlerFirst(handler);
		return this;
	}

	@Override
	public HttpClientOperations addHandlerFirst(String name, ChannelHandler handler) {
		super.addHandlerFirst(name, handler);
		return this;
	}

	@Override
	public HttpClientOperations addHandler(ChannelHandler handler) {
		super.addHandler(handler);
		return this;
	}

	@Override
	public HttpClientOperations addHandler(String name, ChannelHandler handler) {
		super.addHandler(name, handler);
		return this;
	}

	@Override
	public HttpClientOperations replaceHandler(String name, ChannelHandler handler) {
		super.replaceHandler(name, handler);
		return this;
	}

	@Override
	public HttpClientResponse removeHandler(String name) {
		super.removeHandler(name);
		return this;
	}

	@Override
	public HttpClientRequest addHeader(CharSequence name, CharSequence value) {
		if (!hasSentHeaders()) {
			this.requestHeaders.add(name, value);
		}
		else {
			throw new IllegalStateException("Status and headers already sent");
		}
		return this;
	}

	@Override
	public InetSocketAddress address() {
		return ((SocketChannel) channel()).remoteAddress();
	}

	@Override
	public HttpClientRequest chunkedTransfer(boolean chunked) {
		if (!hasSentHeaders() && HttpUtil.isTransferEncodingChunked(nettyRequest) != chunked) {
			requestHeaders.remove(HttpHeaderNames.TRANSFER_ENCODING);
			HttpUtil.setTransferEncodingChunked(nettyRequest, chunked);
		}
		return this;
	}

	@Override
	public HttpClientOperations context(Consumer<NettyContext> contextCallback) {
		try {
			contextCallback.accept(context());
		} catch (Exception e) {
			throw Exceptions.propagate(e);
		}
		return this;
	}

	@Override
	public Map<CharSequence, Set<Cookie>> cookies() {
		ResponseState responseState = this.responseState;
		if (responseState != null) {
			return responseState.cookieHolder.getCachedCookies();
		}
		return null;
	}

	@Override
	public HttpClientRequest followRedirect() {
		redirectable = true;
		return this;
	}

	@Override
	public HttpClientRequest failOnClientError(boolean shouldFail) {
		clientError = shouldFail;
		return this;
	}

	@Override
	public HttpClientRequest failOnServerError(boolean shouldFail) {
		serverError = shouldFail;
		return this;
	}

	@Override
	protected void onInboundCancel() {
		channel().close();
	}

	@Override
	protected void onInboundComplete() {
		if (responseState == null) {
			parentContext().fireContextError(new IOException("Connection closed prematurely"));
			return;
		}
		super.onInboundComplete();
	}

	@Override
	public HttpClientRequest header(CharSequence name, CharSequence value) {
		if (!hasSentHeaders()) {
			this.requestHeaders.set(name, value);
		}
		else {
			throw new IllegalStateException("Status and headers already sent");
		}
		return this;
	}

	@Override
	public HttpClientRequest headers(HttpHeaders headers) {
		if (!hasSentHeaders()) {
			String host = requestHeaders.get(HttpHeaderNames.HOST);
			this.requestHeaders.set(headers);
			this.requestHeaders.set(HttpHeaderNames.HOST, host);
		}
		else {
			throw new IllegalStateException("Status and headers already sent");
		}
		return this;
	}

	@Override
	public boolean isFollowRedirect() {
		return redirectable && redirectedFrom.length <= MAX_REDIRECTS;
	}

	@Override
	public boolean isKeepAlive() {
		ResponseState rs = responseState;
		if (rs != null) {
			return HttpUtil.isKeepAlive(rs.response);
		}
		return HttpUtil.isKeepAlive(nettyRequest);
	}

	@Override
	public boolean isWebsocket() {
		return get(channel()).getClass()
		                     .equals(HttpClientWSOperations.class);
	}

	@Override
	public HttpClientRequest keepAlive(boolean keepAlive) {
		HttpUtil.setKeepAlive(nettyRequest, keepAlive);
		return this;
	}

	@Override
	public HttpMethod method() {
		return nettyRequest.method();
	}

	@Override
	public final HttpClientOperations onClose(Action onClose) {
		super.onClose(onClose);
		return this;
	}

	@Override
	public String[] redirectedFrom() {
		String[] redirectedFrom = this.redirectedFrom;
		String[] dest = new String[redirectedFrom.length];
		System.arraycopy(redirectedFrom, 0, dest, 0, redirectedFrom.length);
		return dest;
	}

	@Override
	public HttpHeaders requestHeaders() {
		return nettyRequest.headers();
	}

	public HttpHeaders responseHeaders() {
		ResponseState responseState = this.responseState;
		if (responseState != null) {
			return responseState.headers;
		}
		else {
			return null;
		}
	}

	@Override
	public Flowable<Void> send() {
		if (markSentHeaderAndBody()) {
			HttpMessage request = newFullEmptyBodyMessage();
			return FutureFlowable.deferFuture(() -> channel().writeAndFlush(request));
		}
		else {
			return Flowable.empty();
		}
	}

	@Override
	public NettyOutbound send(Publisher<? extends ByteBuf> source) {
		if (method() == HttpMethod.GET || method() == HttpMethod.HEAD) {
			ByteBufAllocator alloc = channel().alloc();
			return then(Flowable.fromPublisher(source)
			    .doOnNext(ByteBuf::retain)
			    .collect(alloc::buffer, ByteBuf::writeBytes)
			    .flatMapPublisher(agg -> {
				    if (!hasSentHeaders() && !HttpUtil.isTransferEncodingChunked(
						    outboundHttpMessage()) && !HttpUtil.isContentLengthSet(
						    outboundHttpMessage())) {
					    outboundHttpMessage().headers()
					                         .setInt(HttpHeaderNames.CONTENT_LENGTH,
							                         agg.readableBytes());
				    }
				    return send(Flowable.just(agg)).then();
			    }));
		}
		return super.send(source);
	}

	@Override
	public Flowable<Long> sendForm(Consumer<Form> formCallback) {
		return new FlowableSendForm(this, formCallback);
	}

	@Override
	public WebsocketOutbound sendWebsocket() {
		return sendWebsocket(null);
	}

	@Override
	public WebsocketOutbound sendWebsocket(String subprotocols) {
		Flowable<Void> m = withWebsocketSupport(websocketUri(), subprotocols, noopHandler());

		return new WebsocketOutbound() {

			@Override
			public String selectedSubprotocol() {
				return null;
			}

			@Override
			public NettyContext context() {
				return HttpClientOperations.this;
			}

			@Override
			public Flowable<Void> then() {
				return m;
			}
		};
	}

	@Override
	public Flowable<Void> receiveWebsocket(String protocols,
			BiFunction<? super WebsocketInbound, ? super WebsocketOutbound, ? extends Publisher<Void>> websocketHandler) {
		ObjectHelper.requireNonNull(websocketHandler, "websocketHandler");
		return withWebsocketSupport(websocketUri(), protocols, websocketHandler);
	}

	final URI websocketUri() {
		URI uri;
		try {
			String url = uri();
			if (url.startsWith(HttpClient.HTTP_SCHEME) || url.startsWith(HttpClient.WS_SCHEME)) {
				uri = new URI(url);
			}
			else {
				String host = requestHeaders().get(HttpHeaderNames.HOST);
				uri = new URI((isSecure ? HttpClient.WSS_SCHEME :
						HttpClient.WS_SCHEME) + "://" + host + (url.startsWith("/") ?
						url : "/" + url));
			}

		}
		catch (URISyntaxException e) {
			throw Exceptions.propagate(e);
		}
		return uri;
	}

	@Override
	public WebsocketInbound receiveWebsocket() {
		return null;
	}

	@Override
	public HttpResponseStatus status() {
		ResponseState responseState = this.responseState;
		if (responseState != null) {
			return HttpResponseStatus.valueOf(responseState.response.status()
			                                                        .code());
		}
		return null;
	}

	@Override
	public final String uri() {
		return this.nettyRequest.uri();
	}

	@Override
	public final HttpVersion version() {
		HttpVersion version = this.nettyRequest.protocolVersion();
		if (version.equals(HttpVersion.HTTP_1_0)) {
			return HttpVersion.HTTP_1_0;
		}
		else if (version.equals(HttpVersion.HTTP_1_1)) {
			return HttpVersion.HTTP_1_1;
		}
		throw new IllegalStateException(version.protocolName() + " not supported");
	}

	@Override
	protected void onHandlerStart() {
		applyHandler();
	}

	@Override
	protected void onOutboundComplete() {
		if (isWebsocket() || isInboundCancelled()) {
			return;
		}
		if (markSentHeaderAndBody()) {
			channel().writeAndFlush(newFullEmptyBodyMessage());
		}
		else if (markSentBody()) {
			channel().writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		}
		channel().read();
	}

	@Override
	protected void onOutboundError(Throwable err) {
		if(RxNetty.isPersistent(channel()) && responseState == null){
			parentContext().fireContextError(err);
			onHandlerTerminate();
			return;
		}
		super.onOutboundError(err);
	}

	@Override
	protected void onInboundNext(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) msg;
			if (response.decoderResult()
			            .isFailure()) {
				onInboundError(response.decoderResult()
				                       .cause());
				return;
			}
			if (started) {
				return;
			}
			started = true;
			setNettyResponse(response);

			if (!isKeepAlive()) {
				markPersistent(false);
			}
			if(isInboundCancelled()){
				ReferenceCountUtil.release(msg);
				return;
			}
			if (checkResponseCode(response)) {
				prefetchMore(ctx);
				parentContext().fireContextActive(this);
			}
			if (msg instanceof FullHttpResponse) {
				super.onInboundNext(ctx, msg);
				onHandlerTerminate();
			}
			return;
		}
		if (msg instanceof LastHttpContent) {
			if (!started) {
				return;
			}
			if (msg != LastHttpContent.EMPTY_LAST_CONTENT) {
				super.onInboundNext(ctx, msg);
			}
			//force auto read to enable more accurate close selection now inbound is done
			channel().config().setAutoRead(true);
			onHandlerTerminate();
			return;
		}

		if (!started) {
			return;
		}
		super.onInboundNext(ctx, msg);
		prefetchMore(ctx);
	}

	@Override
	protected HttpMessage outboundHttpMessage() {
		return nettyRequest;
	}

	final boolean checkResponseCode(HttpResponse response) {
		int code = response.status()
		                   .code();
		if (code >= 500) {
			if (serverError){
				Exception ex = new HttpClientException(uri(), response);
				parentContext().fireContextError(ex);
				onHandlerTerminate();
				return false;
			}
			return true;
		}

		if (code >= 400) {
			if (clientError) {
				Exception ex = new HttpClientException(uri(), response);
				parentContext().fireContextError(ex);
				onHandlerTerminate();
				return false;
			}
			return true;
		}
		if (code == 301 || code == 302 && isFollowRedirect()) {
			Exception ex = new RedirectClientException(uri(), response);
			parentContext().fireContextError(ex);
			onHandlerTerminate();
			return false;
		}
		return true;
	}

	@Override
	protected HttpMessage newFullEmptyBodyMessage() {
		HttpRequest request = new DefaultFullHttpRequest(version(), method(), uri());

		request.headers()
		       .set(requestHeaders.remove(HttpHeaderNames.TRANSFER_ENCODING)
		                          .setInt(HttpHeaderNames.CONTENT_LENGTH, 0));
		return request;
	}

	final HttpRequest getNettyRequest() {
		return nettyRequest;
	}

	final void prefetchMore(ChannelHandlerContext ctx) {
		int inboundPrefetch = this.inboundPrefetch - 1;
		if (inboundPrefetch >= 0) {
			this.inboundPrefetch = inboundPrefetch;
			ctx.read();
		}
	}

	final void setNettyResponse(HttpResponse nettyResponse) {
		ResponseState state = responseState;
		if (state == null) {
			this.responseState =
					new ResponseState(nettyResponse, nettyResponse.headers());
		}
	}

	final Flowable<Void> withWebsocketSupport(URI url,
			String protocols,
			BiFunction<? super WebsocketInbound, ? super WebsocketOutbound, ? extends Publisher<Void>> websocketHandler) {

		//prevent further header to be sent for handshaking
		if (markSentHeaders()) {
			addHandlerFirst(NettyPipeline.HttpAggregator, new HttpObjectAggregator(8192));

			HttpClientWSOperations ops = new HttpClientWSOperations(url, protocols, this);

			if (replace(ops)) {
				Flowable<Void> handshake = FutureFlowable.from(ops.handshakerResult)
										                     .ignoreElements()
				                                 .andThen(Flowable.defer(() -> websocketHandler.apply(
						                                 ops,
						                                 ops)));
				if (websocketHandler != noopHandler()) {
					handshake = handshake
							.doOnError(ops)
							.doOnComplete(() -> ops.accept(null));
				}
				return handshake;
			}
		}
		else if (isWebsocket()) {
			HttpClientWSOperations ops =
					(HttpClientWSOperations) get(channel());
			if(ops != null) {
				Flowable<Void> handshake = FutureFlowable.from(ops.handshakerResult);

				if (websocketHandler != noopHandler()) {
					handshake =
							handshake.ignoreElements().andThen(Flowable.defer(() -> websocketHandler.apply(ops, ops)))
									.doOnError(ops)
									.doOnComplete(() -> ops.accept(null));
				}
				return handshake;
			}
		}
		return Flowable.error(new IllegalStateException("Failed to upgrade to websocket"));
	}

	static final class ResponseState {

		final HttpResponse response;
		final HttpHeaders  headers;
		final Cookies      cookieHolder;

		ResponseState(HttpResponse response, HttpHeaders headers) {
			this.response = response;
			this.headers = headers;
			this.cookieHolder = Cookies.newClientResponseHolder(headers);
		}
	}

	static final class FlowableSendForm extends Flowable<Long> {

		static final HttpDataFactory DEFAULT_FACTORY = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

		final HttpClientOperations parent;
		final Consumer<Form>       formCallback;

		FlowableSendForm(HttpClientOperations parent,
										 Consumer<Form> formCallback) {
			this.parent = parent;
			this.formCallback = formCallback;
		}

		@Override
		public void subscribeActual(Subscriber<? super Long> s) {
			if (parent.channel()
			          .eventLoop()
			          .inEventLoop()) {
				_subscribe(s);
			}
			else {
				parent.channel()
				      .eventLoop()
				      .execute(() -> _subscribe(s));
			}
		}

		void _subscribe(Subscriber<? super Long> s) {
			if (!parent.markSentHeaders()) {
				EmptySubscription.error(
						new IllegalStateException("headers have already " + "been sent"), s);
				return;
			}

			HttpDataFactory df = DEFAULT_FACTORY;

			try {
				HttpClientFormEncoder encoder = new HttpClientFormEncoder(df,
						parent.nettyRequest,
						false,
						HttpConstants.DEFAULT_CHARSET,
						HttpPostRequestEncoder.EncoderMode.RFC1738);

				formCallback.accept(encoder);

				encoder = encoder.applyChanges(parent.nettyRequest);
				df = encoder.newFactory;

				if (!encoder.isMultipart()) {
					parent.chunkedTransfer(false);
				}

				parent.addHandlerFirst(NettyPipeline.ChunkedWriter, new ChunkedWriteHandler());

				boolean chunked = HttpUtil.isTransferEncodingChunked(parent.nettyRequest);

				HttpRequest r = encoder.finalizeRequest();

				if (!chunked) {
					HttpUtil.setTransferEncodingChunked(r, false);
					HttpUtil.setContentLength(r, encoder.length());
				}

				ChannelFuture f = parent.channel()
				                        .writeAndFlush(r);

				Flowable<Long> tail = encoder.progressFlowable.onBackpressureLatest();

				if (encoder.cleanOnTerminate) {
					tail = tail.doOnCancel(encoder)
					           .doAfterTerminate(encoder);
				}

				if (encoder.isChunked()) {
					tail.subscribe(s);
					parent.channel()
					      .writeAndFlush(encoder);
				}
				else {
					FutureFlowable.from(f)
					          .cast(Long.class)
					          .switchIfEmpty(Flowable.just(encoder.length()))
					          .subscribe(s);
				}


			}
			catch (Throwable e) {
				Exceptions.throwIfFatal(e);
				df.cleanRequestHttpData(parent.nettyRequest);
				EmptySubscription.error(e, s);
			}
		}
	}

	static final int                    MAX_REDIRECTS      = 50;
	static final String[]               EMPTY_REDIRECTIONS = new String[0];
	static final AttributeKey<String[]> REDIRECT_ATTR_KEY  =
			AttributeKey.newInstance("httpRedirects");
}
