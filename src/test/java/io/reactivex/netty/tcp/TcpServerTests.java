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

package io.reactivex.netty.tcp;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.MaybeSubject;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.NettyInbound;
import io.reactivex.netty.NettyOutbound;
import io.reactivex.netty.NettyPipeline;
import io.reactivex.netty.SocketUtils;
import io.reactivex.netty.http.client.HttpClient;
import io.reactivex.netty.http.server.HttpServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class TcpServerTests {

	ExecutorService threadPool;
	final int msgs    = 10;
	final int threads = 4;

	CountDownLatch latch;
	AtomicLong count = new AtomicLong();
	AtomicLong start = new AtomicLong();
	AtomicLong end   = new AtomicLong();

	@Before
	public void loadEnv() {
		latch = new CountDownLatch(msgs * threads);
		threadPool = Executors.newCachedThreadPool();
	}

	@After
	public void cleanup() {
		threadPool.shutdownNow();
		Schedulers.shutdown();
	}

	@Test
	public void tcpServerHandlesJsonPojosOverSsl() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);

		SslContext clientOptions = SslContextBuilder.forClient()
		                                            .trustManager(
				                                            InsecureTrustManagerFactory.INSTANCE)
		                                            .build();
		final TcpServer server = TcpServer.create(opts -> opts.host("localhost")
		                                                      .sslSelfSigned());

		ObjectMapper m = new ObjectMapper();

		NettyContext connectedServer = server.newHandler((in, out) -> {
			in.receive()
			  .asByteArray()
			  .map(bb -> {
				  try {
					  return m.readValue(bb, Pojo.class);
				  }
				  catch (IOException io) {
					  throw Exceptions.propagate(io);
				  }
			  })
			  .subscribe(data -> {
				  if ("John Doe".equals(data.getName())) {
					  latch.countDown();
				  }
			  });

			return out.sendString(Flowable.just("Hi"))
			          .neverComplete();
		})
		                                     .blockingSingle();

		final TcpClient client = TcpClient.create(opts -> opts.host("localhost")
		                                                      .port(connectedServer.address().getPort())
		                                                      .sslContext(clientOptions));

		NettyContext connectedClient = client.newHandler((in, out) -> {
			//in
			in.receive()
			  .asString()
			  .subscribe(data -> {
				  if (data.equals("Hi")) {
					  latch.countDown();
				  }
			  });

			//out
			return out.send(Flowable.just(new Pojo("John" + " Doe"))
			                    .map(s -> {
				                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					                    m.writeValue(os, s);
					                    return out.alloc()
					                             .buffer()
					                             .writeBytes(os.toByteArray());
				                    }
				                    catch (IOException ioe) {
					                    throw Exceptions.propagate(ioe);
				                    }
			                    }))
			          .neverComplete();
//			return Mono.empty();
		})
		                                     .blockingSingle();

		assertTrue("Latch was counted down", latch.await(5, TimeUnit.SECONDS));

		connectedClient.dispose();
		connectedServer.dispose();
	}

	@Test(timeout = 10000)
	public void testHang() throws Exception {
		NettyContext httpServer = HttpServer
				.create(opts -> opts.host("0.0.0.0").port(0))
				.newRouter(r -> r.get("/data", (request, response) -> {
					return response.send(Flowable.empty());
				})).blockingSingle();
		httpServer.dispose();
	}

	@Test
	public void exposesRemoteAddress() throws InterruptedException {
		final int port = SocketUtils.findAvailableTcpPort();
		final CountDownLatch latch = new CountDownLatch(1);

		NettyContext server = TcpServer.create(port)
		                               .newHandler((in, out) -> {
			                             InetSocketAddress remoteAddr =
					                             in.remoteAddress();
			                             assertNotNull("remote address is not null",
					                             remoteAddr.getAddress());
			                             latch.countDown();

			                             return Flowable.never();
		                             })
		                               .blockingSingle();

		NettyContext client = TcpClient.create(port)
		                               .newHandler((in, out) -> out.sendString(Flowable.just(
				                             "Hello World!")))
		                               .blockingSingle();

		assertTrue("latch was counted down", latch.await(5, TimeUnit.SECONDS));

		client.dispose();
		server.dispose();
	}

	@Test
	public void exposesNettyPipelineConfiguration() throws InterruptedException {
		final int port = SocketUtils.findAvailableTcpPort();
		final CountDownLatch latch = new CountDownLatch(2);

		final TcpClient client = TcpClient.create(port);

		BiFunction<? super NettyInbound, ? super NettyOutbound, ? extends Publisher<Void>>
				serverHandler = (in, out) -> {
			in.receive()
			  .asString()
			  .subscribe(data -> {
				  latch.countDown();
			  });
			return Flowable.never();
		};

		TcpServer server = TcpServer.create(opts -> opts
		                                                 .afterChannelInit(c -> c.pipeline()
		                                                                         .addBefore(
				                                                 NettyPipeline.ReactiveBridge,
				                                                 "codec",
				                                                 new LineBasedFrameDecoder(
						                                                 8 * 1024)))
		                                                 .port(port));

		NettyContext connected = server.newHandler(serverHandler)
		                               .blockingSingle();

		client.newHandler((in, out) -> out.send(Flowable.just("Hello World!\n", "Hello 11!\n")
		                                            .map(b -> out.alloc()
		                                                        .buffer()
		                                                        .writeBytes(b.getBytes()))))
		      .blockingSingle();

		assertTrue("Latch was counted down", latch.await(10, TimeUnit.SECONDS));

		connected.dispose();
	}

	@Test
	public void testIssue462() throws InterruptedException {

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		NettyContext server = TcpServer.create(0)
		                               .newHandler((in, out) -> {
			                             in.receive()
			                               .subscribe(trip -> {
				                               countDownLatch.countDown();
			                               });
			                             return Flowable.never();
		                             })
		                               .blockingSingle();

		System.out.println("PORT +" + server.address()
		                                    .getPort());

		NettyContext client = TcpClient.create(server.address()
		                                             .getPort())
		                               .newHandler((in, out) -> out.sendString(Flowable.just(
				                             "test")))
		                               .blockingSingle();

		client.dispose();
		server.dispose();

		assertThat("countDownLatch counted down",
				countDownLatch.await(5, TimeUnit.SECONDS));
	}

	@Test
	@Ignore
	public void proxyTest() throws Exception {
		HttpServer server = HttpServer.create();
		server.newRouter(r -> r.get("/search/{search}",
				(in, out) -> HttpClient.create()
				                       .get("foaas.herokuapp.com/life/" + in.param(
						                       "search"))
				                       .flatMap(repliesOut -> out.send(repliesOut.receive()))))
		      .blockingSingle()
		      .onClose()
		      .blockingSubscribe();
	}

	@Test
	@Ignore
	public void wsTest() throws Exception {
		HttpServer server = HttpServer.create();
		server.newRouter(r -> r.get("/search/{search}",
				(in, out) -> HttpClient.create()
				                       .get("ws://localhost:3000",
						                       requestOut -> requestOut.sendWebsocket()
						                                               .sendString(Flowable.just("ping")))
				                       .flatMap(repliesOut -> out.sendGroups(repliesOut.receive()
				                                                                       .window(100)))))
		      .blockingSingle()
		      .onClose()
		      .blockingSubscribe();
	}

	@Test
	public void toStringShowsOptions() {
		TcpServer server = TcpServer.create(opt -> opt.host("foo").port(123));

		Assertions.assertThat(server.toString()).isEqualTo("TcpServer: listening on foo:123");
	}

	@Test
	public void gettingOptionsDuplicates() {
		TcpServer server = TcpServer.create(opt -> opt.host("foo").port(123));
		Assertions.assertThat(server.options())
		          .isNotSameAs(server.options)
		          .isNotSameAs(server.options());
	}

	@Test
	public void sendFileSecure()
			throws CertificateException, SSLException, InterruptedException, URISyntaxException {
		File largeFile = new File(getClass().getResource("/largeFile.txt").toURI());
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		SslContext sslServer = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		SslContext sslClient = SslContextBuilder.forClient().trustManager(ssc.cert()).build();

		NettyContext context =
				TcpServer.create(opt -> opt.sslContext(sslServer))
				         .newHandler((in, out) ->
						         in.receive()
						           .asString()
						           .flatMap(word -> "GOGOGO".equals(word) ?
								           out.sendFile(largeFile).then() :
								           out.sendString(Flowable.just("NOPE"))
						           )
				         )
				         .blockingSingle();

		MaybeSubject<String> m1 = MaybeSubject.create();
		MaybeSubject<String> m2 = MaybeSubject.create();

		NettyContext client1 =
				TcpClient.create(opt -> opt.port(context.address().getPort())
				                           .sslContext(sslClient))
				         .newHandler((in, out) -> {
					         in.receive()
					           .asString()
					           .subscribe(m1::onSuccess);

					         return out.sendString(Flowable.just("gogogo"))
							         .neverComplete();
				         })
				         .blockingSingle();

		NettyContext client2 =
				TcpClient.create(opt -> opt.port(context.address().getPort())
				                           .sslContext(sslClient))
				         .newHandler((in, out) -> {
					         in.receive()
					           .asString(StandardCharsets.UTF_8)
					           .take(2)
					           .reduceWith(String::new, String::concat)
					           .subscribe(m2::onSuccess);

					         return out.sendString(Flowable.just("GOGOGO"))
					                   .neverComplete();
				         })
				         .blockingSingle();

		String client1Response = m1.blockingGet();
		String client2Response = m2.blockingGet();

		client1.dispose();
		client1.onClose().blockingSubscribe();

		client2.dispose();
		client2.onClose().blockingSubscribe();

		context.dispose();
		context.onClose().blockingSubscribe();

		Assertions.assertThat(client1Response).isEqualTo("NOPE");

		Assertions.assertThat(client2Response)
		          .startsWith("This is an UTF-8 file that is larger than 1024 bytes. " + "It contains accents like é.")
		          .contains("1024 mark here ->")
		          .contains("<- 1024 mark here")
		          .endsWith("End of File");
	}

	@Test
	public void sendFileChunked() throws InterruptedException, IOException, URISyntaxException {
		File largeFile = new File(getClass().getResource("/largeFile.txt").toURI());
		long fileSize = largeFile.length();

		assertSendFile(out -> out.sendFileChunked(largeFile, 0, fileSize));
	}

	private void assertSendFile(Function<NettyOutbound, NettyOutbound> fn)
			throws InterruptedException {



		NettyContext context =
				TcpServer.create()
				         .newHandler((in, out) ->
						         in.receive()
						           .asString()
						           .flatMap(word -> "GOGOGO".equals(word) ?
								           fn.apply(out).then() :
								           out.sendString(Flowable.just("NOPE"))
						           )
				         )
				         .blockingSingle();

		MaybeSubject<String> m1 = MaybeSubject.create();
		MaybeSubject<String> m2 = MaybeSubject.create();

		NettyContext client1 =
				TcpClient.create(opt -> opt.port(context.address().getPort()))
				         .newHandler((in, out) -> {
					         in.receive()
					           .asString()
					           .subscribe(m1::onSuccess);

					         return out.sendString(Flowable.just("gogogo"))
					                   .neverComplete();
				         })
				         .blockingSingle();

		NettyContext client2 =
				TcpClient.create(opt -> opt.port(context.address().getPort()))
				         .newHandler((in, out) -> {
					         in.receive()
					           .asString(StandardCharsets.UTF_8)
					           .take(2)
					           .reduceWith(String::new, String::concat)
					           .subscribe(m2::onSuccess);

					         return out.sendString(Flowable.just("GOGOGO"))
					                   .neverComplete();
				         })
				         .blockingSingle();

		String client1Response = m1.blockingGet();
		String client2Response = m2.blockingGet();

		client1.dispose();
		client1.onClose().blockingSubscribe();

		client2.dispose();
		client2.onClose().blockingSubscribe();

		context.dispose();
		context.onClose().blockingSubscribe();

		Assertions.assertThat(client1Response).isEqualTo("NOPE");

		Assertions.assertThat(client2Response)
		          .startsWith("This is an UTF-8 file that is larger than 1024 bytes. " + "It contains accents like é.")
		          .contains("1024 mark here ->")
		          .contains("<- 1024 mark here")
		          .endsWith("End of File");
	}

	@Test(timeout = 2000)
	public void startAndAwait() throws InterruptedException {
		AtomicReference<BlockingNettyContext> bnc = new AtomicReference<>();
		CountDownLatch startLatch = new CountDownLatch(1);

		Thread t = new Thread(() -> TcpServer.create()
		                                     .startAndAwait((in, out) -> out.sendString(Flowable.just("foo")),
				v -> {bnc.set(v);
					                                     startLatch.countDown();
				                                     }));
		t.start();
		//let the server initialize
		startLatch.await();

		//check nothing happens for 200ms
		t.join(200);
		Assertions.assertThat(t.isAlive()).isTrue();

		//check that stopping the bnc stops the server
		bnc.get().shutdown();
		t.join();
		Assertions.assertThat(t.isAlive()).isFalse();
	}

	public static class Pojo {

		private String name;

		private Pojo() {
		}

		private Pojo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "Pojo{" + "name='" + name + '\'' + '}';
		}
	}

}
