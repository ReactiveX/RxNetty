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

package io.reactivex.netty.http.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLException;

import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.netty.http.HttpResources;
import io.reactivex.netty.http.client.HttpClient;
import io.reactivex.netty.resources.DefaultPoolResources;
import io.reactivex.netty.resources.PoolResources;
import io.reactivex.netty.tcp.TcpClient;
import org.junit.Assert;
import org.junit.Test;
import io.reactivex.netty.ByteBufFlowable;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.NettyOutbound;
import io.reactivex.netty.http.client.HttpClientResponse;
import io.reactivex.netty.tcp.BlockingNettyContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Maldini
 */
public class HttpServerTests {

	@Test
	public void defaultHttpPort() {
		BlockingNettyContext blockingFacade = HttpServer.create()
		                                                .start((req, resp) -> resp.sendNotFound());
		blockingFacade.shutdown();

		assertThat(blockingFacade.getPort())
				.isEqualTo(8080)
				.isEqualTo(blockingFacade.getContext().address().getPort());
	}

	@Test
	public void defaultHttpPortWithAddress() {
		BlockingNettyContext blockingFacade = HttpServer.create("localhost")
		                                                .start((req, resp) -> resp.sendNotFound());
		blockingFacade.shutdown();

		assertThat(blockingFacade.getPort())
				.isEqualTo(8080)
				.isEqualTo(blockingFacade.getContext().address().getPort());
	}

	@Test
	public void httpPortOptionTakesPrecedenceOverBuilderField() {
		HttpServer.Builder builder = HttpServer.builder()
		                                       .options(o -> o.port(9081))
		                                       .port(9080);
		HttpServer binding = builder.build();
		BlockingNettyContext blockingFacade = binding.start((req, resp) -> resp.sendNotFound());
		blockingFacade.shutdown();

		assertThat(builder).hasFieldOrPropertyWithValue("port", 9080);

		assertThat(blockingFacade.getPort())
				.isEqualTo(9081)
				.isEqualTo(blockingFacade.getContext().address().getPort());


		assertThat(binding.options().getAddress())
				.isInstanceOf(InetSocketAddress.class)
				.hasFieldOrPropertyWithValue("port", 9081);
	}

	@Test
	public void sendFileSecure()
			throws CertificateException, SSLException, URISyntaxException {
		File largeFile = new File(getClass().getResource("/largeFile.txt").toURI());
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		SslContext sslServer = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		SslContext sslClient = SslContextBuilder.forClient().trustManager(ssc.cert()).build();

		NettyContext context =
				HttpServer.create(opt -> opt.sslContext(sslServer))
				          .newHandler((req, resp) -> resp.sendFile(largeFile))
				          .blockingSingle();


		HttpClientResponse response =
				HttpClient.create(opt -> opt.port(context.address().getPort())
				                            .sslContext(sslClient))
				          .get("/foo")
				          .blockingSingle();

		context.dispose();
		context.onClose().ignoreElements().blockingAwait();

		String body = response.receive().aggregate().asString(StandardCharsets.UTF_8).blockingGet();

		assertThat(body)
				.startsWith("This is an UTF-8 file that is larger than 1024 bytes. " + "It contains accents like é.")
				.contains("1024 mark here -><- 1024 mark here")
				.endsWith("End of File");
	}

	@Test
	public void sendFileChunked() throws IOException, URISyntaxException {
		File largeFile = new File(getClass().getResource("/largeFile.txt").toURI());
		long fileSize = largeFile.length();
		assertSendFile(out -> out.sendFileChunked(largeFile, 0, fileSize));
	}

	private void assertSendFile(Function<HttpServerResponse, NettyOutbound> fn) {
		NettyContext context =
				HttpServer.create(opt -> opt.host("localhost"))
				          .newHandler((req, resp) -> fn.apply(resp))
				          .blockingSingle();


		HttpClientResponse response =
				HttpClient.create(opt -> opt.port(context.address().getPort()))
				          .get("/foo")
				          .blockingSingle();

		context.dispose();
		context.onClose().ignoreElements().blockingAwait();

		String body = response.receive().aggregate().asString(StandardCharsets.UTF_8).blockingGet();

		assertThat(body)
				.startsWith("This is an UTF-8 file that is larger than 1024 bytes. " + "It contains accents like é.")
				.contains("1024 mark here -><- 1024 mark here")
				.endsWith("End of File");
	}

	@Test
	public void testRestart() {
		// start a first server with a handler that answers HTTP 200 OK
		NettyContext context = HttpServer.create(8080)
		                                 .newHandler((req, resp) -> resp.status(200)
		                                                                .send())
		                                 .blockingSingle();

		HttpClientResponse response = HttpClient.create(8080).get("/").blockingSingle();

		// checking the response status, OK
		assertThat(response.status().code()).isEqualTo(200);
		// dispose the Netty context and wait for the channel close
		context.dispose();
		context.onClose().ignoreElements().blockingAwait();

		//REQUIRED - bug pool does not detect/translate properly lifecycle
		HttpResources.reset();

		// create a totally new server instance, with a different handler that answers HTTP 201
		context = HttpServer.create(8080)
		                    .newHandler((req, resp) -> resp.status(201).send()).blockingSingle();

		response = HttpClient.create(8080).get("/").blockingSingle();

		// fails, response status is 200 and debugging shows the the previous handler is called
		assertThat(response.status().code()).isEqualTo(201);
		context.dispose();
		context.onClose().ignoreElements().blockingAwait();
	}

	@Test
	public void errorResponseAndReturn() throws Exception {
		NettyContext c = HttpServer.create(0)
		                           .newHandler((req, resp) -> Flowable.error(new Exception("returnError")))
		                           .blockingSingle();

		assertThat(HttpClient.create(c.address()
		                              .getPort())
		                     .get("/return", r -> r.failOnServerError(false))
		                     .blockingSingle()
		                     .status()
		                     .code()).isEqualTo(500);

		c.dispose();

	}

	@Test
	public void httpPipelining() throws Exception {

		AtomicInteger i = new AtomicInteger();

		NettyContext c = HttpServer.create(0)
		                           .newHandler((req, resp) -> resp.header(HttpHeaderNames.CONTENT_LENGTH, "1")
		                                                          .sendString(Flowable.just(i.incrementAndGet())
		                                                                          .flatMap(d -> Flowable.timer(4 - d, TimeUnit.SECONDS)
		                                                                                         .map(x -> d + "\n"))))
		                           .blockingSingle();

		DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
				HttpMethod.GET,
				"/plaintext");

		CountDownLatch latch = new CountDownLatch(6);

		TcpClient.create(c.address()
		                  .getPort())
		         .newHandler((in, out) -> {
			         in.context()
			           .addHandlerFirst(new HttpClientCodec());

			         in.receiveObject()
			           .ofType(DefaultHttpContent.class)
			           .to(ByteBufFlowable::fromInbound)
			           .asString()
			           .map(Integer::parseInt)
			           .subscribe(d -> {
				           for (int x = 0; x < d; x++) {
					           latch.countDown();
				           }
			           });

			         return out.sendObject(Flowable.just(request.retain(),
					         request.retain(),
					         request.retain()))
			                   .neverComplete();
		         })
		         .blockingSingle();

		Assert.assertTrue(latch.await(45, TimeUnit.SECONDS));

	}

	@Test
	public void flushOnComplete() {

		Flowable<String> test = Flowable.range(0, 100)
		                        .map(n -> String.format("%010d", n));

		NettyContext c = HttpServer.create(0)
		                           .newHandler((req, resp) -> resp.sendString(test.map(s -> s + "\n")))
		                           .blockingSingle();

		Flowable<String> client = HttpClient.create(c.address()
		                                         .getPort())
		                                .get("/")
		                                .blockingSingle()
		                                .addHandler(new LineBasedFrameDecoder(10))
		                                .receive()
		                                .asString();

		client.test()
				.awaitDone(30, TimeUnit.SECONDS)
				.assertValueSequence(test.blockingIterable())
				.assertComplete();
	}

	@Test
	public void keepAlive() throws URISyntaxException {
		File resource = new File(getClass().getResource("/public").toURI());
		NettyContext c = HttpServer.create(0)
		                           .newRouter(routes -> routes.directory("/test", resource))
		                           .blockingSingle();

		HttpResources.set(DefaultPoolResources.fixed("http", 1));

		HttpClientResponse response0 = HttpClient.create(c.address()
		                                                  .getPort())
		                                         .get("/test/index.html")
		                                         .blockingSingle();

		HttpClientResponse response1 = HttpClient.create(c.address()
		                                                  .getPort())
		                                         .get("/test/test.css")
		                                         .blockingSingle();

		HttpClientResponse response2 = HttpClient.create(c.address()
		                                                  .getPort())
		                                         .get("/test/test1.css")
		                                         .blockingSingle();

		HttpClientResponse response3 = HttpClient.create(c.address()
		                                                  .getPort())
		                                         .get("/test/test2.css")
		                                         .blockingSingle();

		HttpClientResponse response4 = HttpClient.create(c.address()
		                                                  .getPort())
		                                         .get("/test/test3.css")
		                                         .blockingSingle();

		HttpClientResponse response5 = HttpClient.create(c.address()
		                                                  .getPort())
		                                         .get("/test/test4.css")
		                                         .blockingSingle();

		HttpClientResponse response6 = HttpClient.create(opts -> opts.port(c.address().getPort())
		                                                             .disablePool())
		                                         .get("/test/test5.css")
		                                         .blockingSingle();

		Assert.assertEquals(response0.channel(), response1.channel());
		Assert.assertEquals(response0.channel(), response2.channel());
		Assert.assertEquals(response0.channel(), response3.channel());
		Assert.assertEquals(response0.channel(), response4.channel());
		Assert.assertEquals(response0.channel(), response5.channel());
		Assert.assertNotEquals(response0.channel(), response6.channel());

		HttpResources.reset();
	}


	@Test
	public void toStringShowsOptions() {
		HttpServer server = HttpServer.create(opt -> opt.host("foo")
		                                                .port(123)
		                                                .compression(987));

		assertThat(server.toString()).isEqualTo("HttpServer: listening on foo:123, gzip over 987 bytes");
	}

	@Test
	public void gettingOptionsDuplicates() {
		HttpServer server = HttpServer.create(opt -> opt.host("foo").port(123).compression(true));
		assertThat(server.options())
		          .isNotSameAs(server.options)
		          .isNotSameAs(server.options());
	}

	@Test
	public void startRouter() {
		BlockingNettyContext facade = HttpServer.create(0)
		                                        .startRouter(routes -> routes.get("/hello",
				                                        (req, resp) -> resp.sendString(Flowable.just("hello!"))));

		try {
			assertThat(HttpClient.create(facade.getPort())
			                     .get("/hello")
			                     .blockingSingle()
			                     .status()
			                     .code()).isEqualTo(200);

			assertThat(HttpClient.create(facade.getPort())
			                     .get("/helloMan", req -> req.failOnClientError(false))
			                     .blockingSingle()
			                     .status()
			                     .code()).isEqualTo(404);
		}
		finally {
			facade.shutdown();
		}
	}

	@Test
	public void startRouterAndAwait()
			throws InterruptedException {
		ExecutorService ex = Executors.newSingleThreadExecutor();
		AtomicReference<BlockingNettyContext> ref = new AtomicReference<>();

		Future<?> f = ex.submit(() -> HttpServer.create(0)
		                                        .startRouterAndAwait(routes -> routes.get("/hello", (req, resp) -> resp.sendString(Flowable.just("hello!"))),
				                                        ref::set)
		);

		//if the server cannot be started, a ExecutionException will be thrown instead
		assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> f.get(1, TimeUnit.SECONDS));

		//the router is not done and is still blocking the thread
		assertThat(f.isDone()).isFalse();
		assertThat(ref.get()).isNotNull().withFailMessage("Server is not initialized after 1s");

		//shutdown the router to unblock the thread
		ref.get().shutdown();
		Thread.sleep(100);
		assertThat(f.isDone()).isTrue();
	}

	@Test
	public void nonContentStatusCodes() {
		NettyContext server =
				HttpServer.create(ops -> ops.host("localhost"))
				          .newRouter(r -> r.get("/204-1", (req, res) -> res.status(HttpResponseStatus.NO_CONTENT)
				                                                           .sendHeaders())
				                           .get("/204-2", (req, res) -> res.status(HttpResponseStatus.NO_CONTENT))
				                           .get("/205-1", (req, res) -> res.status(HttpResponseStatus.RESET_CONTENT)
				                                                           .sendHeaders())
				                           .get("/205-2", (req, res) -> res.status(HttpResponseStatus.RESET_CONTENT))
				                           .get("/304-1", (req, res) -> res.status(HttpResponseStatus.NOT_MODIFIED)
				                                                           .sendHeaders())
				                           .get("/304-2", (req, res) -> res.status(HttpResponseStatus.NOT_MODIFIED)))
				          .blockingSingle();

		int port = server.address().getPort();
		checkResponse("/204-1", port);
		checkResponse("/204-2", port);
		checkResponse("/205-1", port);
		checkResponse("/205-2", port);
		checkResponse("/304-1", port);
		checkResponse("/304-2", port);

		server.dispose();
	}

	private void checkResponse(String url, int port) {
		Flowable<HttpHeaders> response =
				HttpClient.create(ops -> ops.port(port))
				          .get(url)
				          .flatMap(res -> Flowable.just(res.responseHeaders()));

		response.test()
				.awaitDone(30, TimeUnit.SECONDS)
				.assertValue(h -> !h.contains("Transfer-Encoding") &&
		                       h.contains("Content-Length") &&
		                       Integer.parseInt(h.get("Content-Length")) == 0)
				.assertComplete();
	}
}
