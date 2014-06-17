package io.reactivex.netty.pipeline.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomasz Bak
 */
public class SslConfiguration {

    private boolean clientMode;
    private boolean wantClientAuth;
    private String[] protocols;
    private String[] cipherSuite;
    private final KeyManagerFactory keyManagerFactory;
    private final TrustManagerFactory trustManagerFactory;

    SslConfiguration(boolean userMode, boolean wantClientAuth, String[] protocols, String[] cipherSuite,
                     KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory) {
        this.clientMode = userMode;
        this.wantClientAuth = wantClientAuth;
        this.protocols = protocols;
        this.cipherSuite = cipherSuite;
        this.keyManagerFactory = keyManagerFactory;
        this.trustManagerFactory = trustManagerFactory;
    }

    public boolean isClientMode() {
        return clientMode;
    }

    public boolean isWantClientAuth() {
        return wantClientAuth;
    }

    public KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

    public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
    }

    public String[] getProtocols() {
        return protocols;
    }

    public String[] getCipherSuite() {
        return cipherSuite;
    }

    public static SslConfiguration trustAllClient() {
        return new SslConfigurationBuilder().withUserMode().withTrustAllManagerFactory().build();
    }

    public static SslConfiguration selfSignedServer() {
        return new SslConfigurationBuilder().withSelfSignedCertificate().build();
    }

    public static SslConfiguration defaultTrustStoreClient() {
        return new SslConfigurationBuilder().withUserMode().withDefaultTrustManagerFactory().build();
    }

    public static class SslConfigurationBuilder {

        protected File certChainFile;
        protected File keyFile;
        protected String keyPassword;

        private boolean userMode;
        private boolean wantClientAuth;
        protected TrustManagerFactory trustManagerFactory;
        private boolean defaultTrustManagerFactory;
        protected KeyManagerFactory keyManagerFactory;
        private boolean selfSignedCertificate;
        private String[] protocols;
        private String[] cipherSuites;

        public SslConfigurationBuilder withUserMode() {
            this.userMode = true;
            return this;
        }

        public SslConfigurationBuilder withWantClientAuth() {
            this.wantClientAuth = true;
            return this;
        }

        public SslConfigurationBuilder withProtocols(String... protocols) {
            this.protocols = protocols;
            return this;
        }

        public SslConfigurationBuilder withTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
            this.trustManagerFactory = trustManagerFactory;
            return this;
        }

        public SslConfigurationBuilder withTrustAllManagerFactory() {
            this.trustManagerFactory = InsecureTrustManagerFactory.INSTANCE;
            return this;
        }

        public SslConfigurationBuilder withDefaultTrustManagerFactory() {
            this.defaultTrustManagerFactory = true;
            return this;
        }

        public SslConfigurationBuilder withKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
            this.keyManagerFactory = keyManagerFactory;
            return this;
        }

        public SslConfigurationBuilder withCertChainFile(File certChainFile) {
            this.certChainFile = certChainFile;
            return this;
        }

        public SslConfigurationBuilder withKeyFile(File keyFile) {
            this.keyFile = keyFile;
            return this;
        }

        public SslConfigurationBuilder withKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }

        public SslConfigurationBuilder withSelfSignedCertificate() {
            this.selfSignedCertificate = true;
            return this;
        }

        public SslConfigurationBuilder withCipherSuites(String... cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }

        public SslConfiguration build() {
            return userMode ? buildUserMode() : buildServerMode();
        }

        private SslConfiguration buildUserMode() {
            TrustManagerFactory tmf = buildTrustManagerFactory();
            if (tmf == null) {
                throw new IllegalStateException("Trust manager factory must be provided for user mode SSL endpoint");
            }
            return new SslConfiguration(userMode, false, protocols, cipherSuites, null, trustManagerFactory);
        }

        private SslConfiguration buildServerMode() {
            KeyManagerFactory kmf = buildKeyManagerFactory();
            TrustManagerFactory tmf = buildTrustManagerFactory();
            if (wantClientAuth && tmf == null) {
                throw new IllegalStateException("Wants client authentication but no trust manager factory set");
            }
            return new SslConfiguration(userMode, wantClientAuth, protocols, cipherSuites, kmf, tmf);
        }

        private KeyManagerFactory buildKeyManagerFactory() {
            boolean fromFiles = isKeyFromFile();
            if (!fromFiles && keyManagerFactory == null && !selfSignedCertificate) {
                throw new IllegalStateException("No private key configured for sever mode SSL endpoint");
            }
            if (keyManagerFactory != null && (fromFiles || selfSignedCertificate) ||
                    fromFiles && selfSignedCertificate) {
                throw new IllegalStateException("Ambiguous setup - multiple private key sources configured");
            }
            KeyManagerFactory kmf = keyManagerFactory;
            if (selfSignedCertificate) {
                kmf = buildSelfSignedCertificateKMF();
            } else if (fromFiles) {
                kmf = buildKmfFromFiles(certChainFile, keyFile);
            }
            return kmf;
        }

        private boolean isKeyFromFile() {
            boolean fromFiles = certChainFile != null || keyFile != null;
            if (fromFiles) {
                if (certChainFile == null) {
                    throw new IllegalStateException("Key file configured without accompanying certificate chain file");
                }
                if (keyFile == null) {
                    throw new IllegalStateException("Certificate chain file configured without accompanying key file");
                }
            }
            return fromFiles;
        }

        /**
         * This block of code is taken from Netty's io.netty.handler.ssl.JdkSslServerContext.
         * Netty's SslContext does not fulfill all the RxNetty's requirements, so we cannot reuse it directly.
         */
        private KeyManagerFactory buildKmfFromFiles(File certChainFile, File keyFile) {
            if (keyPassword == null) {
                keyPassword = "password"; // It cannot be null
            }
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(null, null);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                KeyFactory rsaKF = KeyFactory.getInstance("RSA");
                KeyFactory dsaKF = KeyFactory.getInstance("DSA");

                ByteBuf encodedKeyBuf = PemReader.readPrivateKey(keyFile);
                byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
                encodedKeyBuf.readBytes(encodedKey).release();
                PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(encodedKey);

                PrivateKey key;
                try {
                    key = rsaKF.generatePrivate(encodedKeySpec);
                } catch (InvalidKeySpecException ignore) {
                    key = dsaKF.generatePrivate(encodedKeySpec);
                }

                List<Certificate> certChain = new ArrayList<Certificate>();
                ByteBuf[] certs = PemReader.readCertificates(certChainFile);
                try {
                    for (ByteBuf buf : certs) {
                        certChain.add(cf.generateCertificate(new ByteBufInputStream(buf)));
                    }
                } finally {
                    for (ByteBuf buf : certs) {
                        buf.release();
                    }
                }

                ks.setKeyEntry("key", key, keyPassword.toCharArray(), certChain.toArray(new Certificate[certChain.size()]));

                // Set up key manager factory to use our key store
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, keyPassword.toCharArray());
                return kmf;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create key store with self signed certificate", e);
            }
        }

        private KeyManagerFactory buildSelfSignedCertificateKMF() {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                return buildKmfFromFiles(ssc.certificate(), ssc.privateKey());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create key manager factory with self signed certificate", e);
            }
        }

        private TrustManagerFactory buildTrustManagerFactory() {
            if(trustManagerFactory == null && !defaultTrustManagerFactory) {
                return null;
            }
            if (trustManagerFactory != null && defaultTrustManagerFactory) {
                throw new IllegalStateException("Ambiguous setup - both custom and default trust manager factory configured");
            }
            return defaultTrustManagerFactory ? buildDefaultTrustManagerFactory() : trustManagerFactory;
        }

        private TrustManagerFactory buildDefaultTrustManagerFactory() {
            try {
                KeyStore jks = KeyStore.getInstance("JKS");
                jks.load(null);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(jks);
                return tmf;
            } catch (Exception e) {
                throw new IllegalStateException("Couldn't create default TrustManagerFactory", e);
            }
        }

    }
}
