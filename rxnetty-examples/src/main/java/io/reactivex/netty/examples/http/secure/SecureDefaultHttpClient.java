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

package io.reactivex.netty.examples.http.secure;

import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.client.HttpClient;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;

/**
 * An example showing how to setup an HttpClient that uses the
 * <a href="https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLContext.html#getDefault--">Default SSLContext</a>
 * to make an example request against a secure host on the public internet.
 * <p>
 * For most people, this will be using the default Protocols, CipherSuites and Certificate bundles that come with the
 * system and/or JDK. This should be doing the 'right thing' in most cases, but of course, this depends on the specifics
 * of how the system and/or JDK has been installed or configured.
 * </p>
 * <h2>IMPORTANT:</h2>
 * We are not security experts and you should do your own research or consult an expert when configuring your own
 * secure HttpClients for Production use.
 */
public final class SecureDefaultHttpClient {

    private final static String HOST = "www.netflix.com";
    private final static Integer PORT = 443;

    public static void main(String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(SecureDefaultHttpClient.class);
        Logger logger = env.getLogger();

        SSLEngine sslEngine = null;
        try {
            sslEngine = defaultSSLEngineForClient();
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("Failed to create SSLEngine.", nsae);
            System.exit(-1);
        }

        HttpClient.newClient(HOST, PORT)
                .enableWireLogging("http-secure-default-client", LogLevel.DEBUG)
                .secure(sslEngine)
                .createGet("/")
                .doOnNext(resp -> logger.info(resp.toString()))
                .flatMap(resp -> {
                            System.out.println(resp.getStatus());
                            return resp.getContent()
                                    .map(bb -> bb.toString(Charset.defaultCharset()));
                        }
                )
                .toBlocking()
                .forEach(logger::info);
    }

    private static SSLEngine defaultSSLEngineForClient() throws NoSuchAlgorithmException {
        SSLContext sslCtx = SSLContext.getDefault();
        SSLEngine sslEngine = sslCtx.createSSLEngine(HOST, PORT);
        sslEngine.setUseClientMode(true);
        return sslEngine;
    }
}
