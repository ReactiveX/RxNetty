/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.reactivex.netty.protocol.http.ws.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

public class WSEagerInputSubscriptionHandlerTest {

    @Test(timeout = 60000)
    public void testHandlerSubscribesEagerly() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server =
                HttpServer.newServer()
                          .start(new RequestHandler<ByteBuf, ByteBuf>() {
                              @Override
                              public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                             HttpServerResponse<ByteBuf> response) {
                                  if (request.isWebSocketUpgradeRequested()) {
                                      return response.acceptWebSocketUpgrade(
                                              new WebSocketHandler() {
                                                  @Override
                                                  public Observable<Void> handle(WebSocketConnection wsConnection) {
                                                      wsConnection.getInput().subscribe();
                                                      return Observable.never();
                                                  }
                                              });
                                  } else {
                                      return response.setStatus(HttpResponseStatus.NOT_FOUND);
                                  }
                              }
                          });

        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        HttpClient.newClient(server.getServerAddress())
                  .createGet("/ws")
                  .requestWebSocketUpgrade()
                  .flatMap(new Func1<WebSocketResponse<ByteBuf>, Observable<Void>>() {
                      @Override
                      public Observable<Void> call(WebSocketResponse<ByteBuf> wsResp) {
                          if (wsResp.isUpgraded()) {
                              return Observable.empty();
                          }
                          return Observable.error(new IllegalStateException("WebSocket upgrade not accepted."));
                      }
                  })
                  .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();
    }
}
