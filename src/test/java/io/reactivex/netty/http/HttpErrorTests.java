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

package io.reactivex.netty.http;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.netty.FutureFlowable;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.http.client.HttpClient;
import io.reactivex.netty.http.server.HttpServer;
import org.junit.Assert;
import org.junit.Test;
import io.reactivex.netty.http.client.HttpClientResponse;

/**
 * @author tokuhirom
 */
public class HttpErrorTests {

	@Test
	public void test() {
		NettyContext server = HttpServer.create(0)
		                                .newRouter(httpServerRoutes -> httpServerRoutes.get(
				                                "/",
				                                (httpServerRequest, httpServerResponse) -> {
					                                return httpServerResponse.sendString(
							                                Flowable.error(new IllegalArgumentException()));
				                                }))
		                                .blockingSingle();

		HttpClient client = HttpClient.create(opt -> opt.host("localhost")
		                                                .port(server.address().getPort())
		                                                .disablePool());

		HttpClientResponse r = client.get("/")
		                             .blockingSingle();

		List<String> result = r.receive()
		                    .asString(StandardCharsets.UTF_8)
		                    .toList()
		                    .blockingGet();

		System.out.println("END");

		FutureFlowable.from(r.context().channel().closeFuture()).ignoreElements().blockingAwait(30, TimeUnit.SECONDS);

		Assert.assertTrue(result.isEmpty());
		Assert.assertTrue(r.isDisposed());
		server.dispose();
	}
}