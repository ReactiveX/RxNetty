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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Flowable;
import io.reactivex.netty.http.server.HttpServer;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.ReplayProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import io.reactivex.netty.NettyContext;

import static org.hamcrest.CoreMatchers.is;

/**
 * @author tjreactive
 * @author smaldini
 */
public class WebsocketTest {

	static final String auth = "bearer abc";

	NettyContext httpServer = null;

	@After
	public void disposeHttpServer() {
		if (httpServer != null)
			httpServer.dispose();
	}

	@Test
	public void simpleTest() {
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket((i, o) -> o.sendString(
				                       Flowable.just("test"))))
		                       .blockingSingle();

		String res = HttpClient.create(httpServer.address()
		                                  .getPort())
		                .get("/test",
				                out -> out.addHeader("Authorization", auth)
				                          .sendWebsocket())
		                .flatMap(in -> in.receive()
		                                 .asString())
		                .toList()
		                .blockingGet()
		                .get(0);

		Assert.assertThat(res, is("test"));
	}

//	static final byte[] testData;
//
//	static {
//		testData = new byte[10000];
//		for (int i = 0; i < testData.length; i++) {
//			testData[i] = 88;
//		}
//
//	}
//
//	@Test
//	public void largeChunk() throws Exception {
//		httpServer = HttpServer.create(0)
//		                       .newHandler((in, out) -> out.sendWebsocket((i, o) -> o
//				                       .sendByteArray(Mono.just(testData))
//		                                                                             .neverComplete()))
//		                       .block(Duration.ofSeconds(30));
//
//		HttpClient.create(httpServer.address()
//		                                         .getPort())
//		                       .get("/test",
//				                       out -> out.addHeader("Authorization", auth)
//				                                 .sendWebsocket())
//		                       .flatMapMany(in -> in.receiveWebsocket()
//		                                        .receive()
//		                                        .asByteArray())
//		                       .doOnNext(d -> System.out.println(d.length))
//		                       .log()
//		                       .subscribe();
//
//		Thread.sleep(200000);
//	}

	@Test
	public void unidirectional() {
		int c = 10;
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket(
				                       (i, o) -> o.options(opt -> opt.flushOnEach())
				                                  .sendString(
																							Flowable.just("test")
						                                      .delay(100, TimeUnit.MILLISECONDS)
						                                      .repeat())))
		                       .blockingSingle();

		Flowable<String> ws = HttpClient.create(httpServer.address()
		                                              .getPort())
		                            .ws("/")
		                            .flatMap(in -> in.receiveWebsocket()
		                                             .aggregateFrames()
		                                             .receive()
		                                             .asString());

		ws.take(c)
				.test()
				.awaitDone(30, TimeUnit.SECONDS)
				.assertValueSequence(Flowable.range(1, c)
																.map(v -> "test")
																.blockingIterable())
				.assertComplete();
	}


	@Test
	public void unidirectionalBinary() {
		int c = 10;
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket(
				                       (i, o) -> o.options(opt -> opt.flushOnEach())
				                                  .sendByteArray(
																							Flowable.just("test".getBytes())
						                                      .delay(100, TimeUnit.MILLISECONDS)
						                                      .repeat())))
		                       .blockingSingle();

		Flowable<String> ws = HttpClient.create(httpServer.address()
		                                              .getPort())
		                            .ws("/")
		                            .flatMap(in -> in.receiveWebsocket()
		                                             .aggregateFrames()
		                                             .receive()
		                                             .asString());

		ws.take(c)
				.test()
				.awaitDone(30, TimeUnit.SECONDS)
				.assertValueSequence(Flowable.range(1, c)
																.map(v -> "test")
																.blockingIterable())
				.assertComplete();
	}

	@Test
	public void duplexEcho() throws Exception {

		int c = 10;
		CountDownLatch clientLatch = new CountDownLatch(c);
		CountDownLatch serverLatch = new CountDownLatch(c);

		FlowableProcessor<String> server =
				ReplayProcessor.<String>create().toSerialized();
		FlowableProcessor<String> client =
				ReplayProcessor.<String>create().toSerialized();

		server.subscribe(v -> serverLatch.countDown());
		client.subscribe(v -> clientLatch.countDown());

		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket((i, o) -> o.sendString(
				                       i.receive()
				                        .asString()
				                        .take(c)
				                        .subscribeWith(server))))
		                       .blockingSingle();

		Flowable.interval(200, TimeUnit.MILLISECONDS)
		    .map(Object::toString)
		    .subscribe(client::onNext);

		HttpClient.create(httpServer.address()
		                            .getPort())
		          .ws("/test")
		          .flatMap(in -> in.receiveWebsocket((i, o) -> o.options(opt -> opt.flushOnEach())
		                                                     .sendString(i.receive()
		                                                                  .asString()
		                                                                  .subscribeWith(
				                                                                  client))))
		          .subscribe();

		Assert.assertTrue(serverLatch.await(10, TimeUnit.SECONDS));
		Assert.assertTrue(clientLatch.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void simpleSubprotocolServerNoSubprotocol() throws Exception {
		httpServer = HttpServer.create(0)
		                                    .newHandler((in, out) -> out.sendWebsocket((i, o) -> o.sendString(
				                                    Flowable.just("test"))))
		                                    .blockingSingle();

		HttpClient.create(
				httpServer.address().getPort())
							.get("/test",
									out -> out.addHeader("Authorization", auth)
														.sendWebsocket("SUBPROTOCOL,OTHER"))
							.flatMap(in -> in.receive().asString())
				.test()
				.awaitDone(30, TimeUnit.SECONDS)
				.assertErrorMessage("Invalid subprotocol. Actual: null. Expected one of: SUBPROTOCOL,OTHER");
	}

	@Test
	public void simpleSubprotocolServerNotSupported() throws Exception {
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket(
				                       "protoA,protoB",
				                       (i, o) -> o.sendString(Flowable.just("test"))))
		                       .blockingSingle();

		HttpClient.create(
				httpServer.address().getPort())
							.get("/test",
									out -> out.addHeader("Authorization", auth)
														.sendWebsocket("SUBPROTOCOL,OTHER"))
							.flatMap(in -> in.receive().asString())
				.test()
				.awaitDone(30, TimeUnit.SECONDS)
				//the SERVER returned null which means that it couldn't select a protocol
				.assertErrorMessage("Invalid subprotocol. Actual: null. Expected one of: SUBPROTOCOL,OTHER");
	}

	@Test
	public void simpleSubprotocolServerSupported() throws Exception {
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket(
				                       "SUBPROTOCOL",
				                       (i, o) -> o.sendString(
						                       Flowable.just("test"))))
		                       .blockingSingle();

		String res = HttpClient.create(httpServer.address().getPort())
		                       .get("/test",
				                out -> out.addHeader("Authorization", auth)
				                          .sendWebsocket("SUBPROTOCOL,OTHER"))
		                .flatMap(in -> in.receive().asString()).toList().blockingGet().get(0);

		Assert.assertThat(res, is("test"));
	}

	@Test
	public void simpleSubprotocolSelected() throws Exception {
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket(
				                       "NOT, Common",
				                       (i, o) -> o.sendString(
						                       Flowable.just("SERVER:" + o.selectedSubprotocol()))))
		                       .blockingSingle();

		String res = HttpClient.create(httpServer.address().getPort())
		                       .get("/test",
				                out -> out.addHeader("Authorization", auth)
				                          .sendWebsocket("Common,OTHER"))
		                       .map(HttpClientResponse::receiveWebsocket)
		                       .flatMap(in -> in.receive().asString()
				                       .map(srv -> "CLIENT:" + in.selectedSubprotocol() + "-" + srv))
		                       .toList().blockingGet().get(0);

		Assert.assertThat(res, is("CLIENT:Common-SERVER:Common"));
	}

	@Test
	public void noSubprotocolSelected() {
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket((i, o) -> o.sendString(
				                       Flowable.just("SERVER:" + o.selectedSubprotocol()))))
		                       .blockingSingle();

		String res = HttpClient.create(httpServer.address()
		                                         .getPort())
		                       .get("/test",
				                       out -> out.addHeader("Authorization", auth)
				                                 .sendWebsocket())
		                       .map(HttpClientResponse::receiveWebsocket)
		                       .flatMap(in -> in.receive()
		                                        .asString()
		                                        .map(srv -> "CLIENT:" + in.selectedSubprotocol() + "-" + srv))
		                       .toList()
		                       .blockingGet()
		                       .get(0);

		Assert.assertThat(res, is("CLIENT:null-SERVER:null"));
	}

	@Test
	public void anySubprotocolSelectsFirstClientProvided() {
		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket("proto2,*", (i, o) -> o.sendString(
				                       Flowable.just("SERVER:" + o.selectedSubprotocol()))))
		                       .blockingSingle();

		String res = HttpClient.create(httpServer.address()
		                                         .getPort())
		                       .get("/test",
				                       out -> out.addHeader("Authorization", auth)
				                                 .sendWebsocket("proto1, proto2"))
		                       .map(HttpClientResponse::receiveWebsocket)
		                       .flatMap(in -> in.receive()
		                                        .asString()
		                                        .map(srv -> "CLIENT:" + in.selectedSubprotocol() + "-" + srv))
		                       .toList()
		                       .blockingGet()
		                       .get(0);

		Assert.assertThat(res, is("CLIENT:proto1-SERVER:proto1"));
	}

	@Test
	public void sendToWebsocketSubprotocol() throws InterruptedException {
		AtomicReference<String> serverSelectedProtocol = new AtomicReference<>();
		AtomicReference<String> clientSelectedProtocol = new AtomicReference<>();
		AtomicReference<String> clientSelectedProtocolWhenSimplyUpgrading = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		httpServer = HttpServer.create(0)
		                       .newHandler((in, out) -> out.sendWebsocket(
		                       		"not,proto1", (i, o) -> {
					                       serverSelectedProtocol.set(i.selectedSubprotocol());
					                       latch.countDown();
					                       return i.receive()
					                               .asString()
					                               .doOnNext(System.err::println)
					                               .ignoreElements()
																		     .toFlowable();
				                       })
		                       )
		                       .blockingSingle();

		HttpClient.create(httpServer.address()
		                            .getPort())
		          .ws("/test", "proto1,proto2")
		          .flatMap(in -> {
			          clientSelectedProtocolWhenSimplyUpgrading.set(in.receiveWebsocket().selectedSubprotocol());
			          return in.receiveWebsocket((i, o) -> {
				          clientSelectedProtocol.set(o.selectedSubprotocol());
				          return o.sendString(Flowable.just("HELLO" + o.selectedSubprotocol()));
			          });
		          })
							.blockingSubscribe();

		Assert.assertTrue(latch.await(30, TimeUnit.SECONDS));
		Assert.assertThat(serverSelectedProtocol.get(), is("proto1"));
		Assert.assertThat(clientSelectedProtocol.get(), is("proto1"));
		Assert.assertThat(clientSelectedProtocolWhenSimplyUpgrading.get(), is("proto1"));
	}

}
