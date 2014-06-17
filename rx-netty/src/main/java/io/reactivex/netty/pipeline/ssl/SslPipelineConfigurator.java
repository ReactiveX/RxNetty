package io.reactivex.netty.pipeline.ssl;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

/**
 * @author Tomasz Bak
 */
public class SslPipelineConfigurator<I, O> implements PipelineConfigurator<I, O> {
    private static final java.lang.String PROTOCOL = "TLS";

    private SslConfiguration sslConfiguration;

    public SslPipelineConfigurator(SslConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst(new SslHandler(createSSLEngine()));
    }

    protected SSLEngine createSSLEngine() {
        SSLEngine engine = context().createSSLEngine();
        if(sslConfiguration.getCipherSuite() != null) {
            engine.setEnabledCipherSuites(sslConfiguration.getCipherSuite());
        }
        if(sslConfiguration.getProtocols() != null) {
            engine.setEnabledProtocols(sslConfiguration.getProtocols());
        }
        engine.setUseClientMode(sslConfiguration.isClientMode());
        engine.setWantClientAuth(sslConfiguration.isWantClientAuth());
        return engine;
    }

    private SSLContext context() {
        try {
            SSLContext ctx = SSLContext.getInstance(PROTOCOL);
            KeyManager[] keyManagers = sslConfiguration.getKeyManagerFactory() == null ? null : sslConfiguration.getKeyManagerFactory().getKeyManagers();
            TrustManager[] trustManagers = sslConfiguration.getTrustManagerFactory() == null ? null : sslConfiguration.getTrustManagerFactory().getTrustManagers();
            ctx.init(keyManagers, trustManagers, null);
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate JDK SSL context", e);
        }
    }

}
