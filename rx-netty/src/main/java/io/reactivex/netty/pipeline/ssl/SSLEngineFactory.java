package io.reactivex.netty.pipeline.ssl;

import io.netty.buffer.ByteBufAllocator;

import javax.net.ssl.SSLEngine;

/**
 * @author Tomasz Bak
 */
public interface SSLEngineFactory {

    SSLEngine createSSLEngine(ByteBufAllocator allocator);

}
