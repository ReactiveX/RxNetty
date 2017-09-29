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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.http.server.HttpServer;
import io.reactivex.netty.http.websocket.WebsocketOutbound;
import org.junit.Test;

/**
 * @author Violeta Georgieva
 */
public class HttpClientWSOperationsTest {

	@Test
	public void failOnClientErrorDisabled() {
		failOnClientServerError(false, false, 401, "", "");
	}

	@Test
	public void failOnClientErrorEnabled() {
		failOnClientServerError(true, false, 401, "", "");
	}

	@Test
	public void failOnServerErrorDisabled() {
		failOnClientServerError(false, false, 500, "", "");
	}

	@Test
	public void failOnServerErrorEnabled() {
		failOnClientServerError(false, true, 500, "", "");
	}

	@Test
	public void failOnServerErrorDisabledFailedNegotiation() {
		failOnClientServerError(false, false, 200, "Server-Protocol", "Client-Protocol");
	}

	@Test
	public void failOnServerErrorEnabledFailedNegotiation() {
		failOnClientServerError(false, true, 200, "Server-Protocol", "Client-Protocol");
	}

	private void failOnClientServerError(boolean clientError, boolean serverError,
			int serverStatus, String serverSubprotocol, String clientSubprotocol) {
		NettyContext httpServer = HttpServer.create(0).newRouter(
			routes -> routes.post("/login", (req, res) -> {
						res.status(serverStatus);
						return res.sendHeaders();
					})
					.get("/ws", (req, res) -> {
						int token = Integer.parseInt(req.requestHeaders().get("Authorization"));
						if (token >= 400) {
							res.status(token);
							return res.send();
						}
						return res.sendWebsocket(serverSubprotocol, (i, o) -> o.sendString(Flowable.just("test")));
					})
			)
			.blockingSingle();

		Flowable<HttpClientResponse> response =
			HttpClient.create(httpServer.address().getPort())
			          .get("/ws", request -> Flowable.just(request.failOnClientError(clientError)
			                                                  .failOnServerError(serverError))
			                                     .to(req -> doLoginFirst(req, httpServer.address().getPort()))
			                                     .flatMap(req -> ws(req, clientSubprotocol)))
			          .switchIfEmpty(Flowable.error(new Exception()));


		if (clientError || serverError) {
			response.test()
					.awaitDone(30, TimeUnit.SECONDS)
					.assertError(Exception.class);
		}
		else {
			response.test()
					.awaitDone(30, TimeUnit.SECONDS)
					.assertValue(res ->
							res.status().code() == (serverStatus == 200 ? 101 : serverStatus))
					.assertComplete();
		}
	}

	private Flowable<HttpClientRequest> doLoginFirst(Flowable<HttpClientRequest> request, int port) {
		return request.zipWith(login(port), (req, auth) -> {
				 req.addHeader("Authorization", auth);
				 return req;
		 });
	}

	private Flowable<String> login(int port) {
		return HttpClient.create(port)
		                 .post("/login", req -> req.failOnClientError(false)
		                                           .failOnServerError(false))
		                 .map(res -> res.status().code() + "");
	}

	private WebsocketOutbound ws(HttpClientRequest request, String clientSubprotocol) {
		return request.sendWebsocket(clientSubprotocol);
	}
}
