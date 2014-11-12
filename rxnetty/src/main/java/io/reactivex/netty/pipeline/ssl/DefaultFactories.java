/*
 * Copyright 2014 Netflix, Inc.
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
 */
package io.reactivex.netty.pipeline.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author Tomasz Bak
 * @author Andrew Reitz
 */
public final class DefaultFactories {

    private static SSLEngineFactory SELF_SIGNED;

    private static SSLEngineFactory TRUST_ALL;

    private DefaultFactories() {
    }

    public static SSLEngineFactory fromSSLContext(SSLContext sslContext) {
        return new SSLContextBasedFactory(sslContext);
    }

    /**
     * Get a SSLEngineFactory configured with a temporary self-signed certificate for testing purposes.
     */
    public static SSLEngineFactory selfSigned() {
        if (SELF_SIGNED == null) {
            synchronized (SelfSignedSSLEngineFactory.class) {
                SELF_SIGNED = new SelfSignedSSLEngineFactory();
            }
        }
        return SELF_SIGNED;
    }

    /**
     * Get a SSLEngineFactory configured to trust all X.509 certificates without any verification.
     */
    public static SSLEngineFactory trustAll() {
        if (TRUST_ALL == null) {
            synchronized (TrustAllSSLEngineFactory.class) {
                TRUST_ALL = new TrustAllSSLEngineFactory();
            }
        }
        return TRUST_ALL;
    }

    public static class SSLContextBasedFactory implements SSLEngineFactory {

        private final SSLContext sslContext;

        public SSLContextBasedFactory(SSLContext sslContext) {
            this.sslContext = sslContext;
        }

        @Override
        public SSLEngine createSSLEngine(ByteBufAllocator allocator) {
            return sslContext.createSSLEngine();
        }
    }

    private static class TrustAllSSLEngineFactory implements SSLEngineFactory {

        private final SslContext sslCtx;

        private TrustAllSSLEngineFactory() {
            try {
                sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            } catch (SSLException e) {
                throw new IllegalStateException("Failed to create Netty's Ssl context with InsecureTrustManagerFactory", e);
            }
        }

        @Override
        public SSLEngine createSSLEngine(ByteBufAllocator allocator) {
            return sslCtx.newEngine(allocator);
        }
    }

    private static class SelfSignedSSLEngineFactory implements SSLEngineFactory {

        private final SslContext sslCtx;

        private SelfSignedSSLEngineFactory() {
            SelfSignedCertificate ssc;
            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                throw new IllegalStateException("Self signed certificate creation error", e);
            }
            try {
                sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } catch (SSLException e) {
                throw new IllegalStateException("Failed to create Netty's Ssl context with self signed certificate", e);
            }
        }

        @Override
        public SSLEngine createSSLEngine(ByteBufAllocator allocator) {
            return sslCtx.newEngine(allocator);
        }
    }
}
