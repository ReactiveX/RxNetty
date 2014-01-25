package io.reactivex.netty.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.Charset;

/**
 * An implementation of {@link PipelineConfigurator} to configure {@link StringEncoder} and {@link StringDecoder} to
 * convert incoming {@link ByteBuf} into {@link String} and outgoing {@link String} as {@link ByteBuf}
 *
 * @see {@link StringEncoder}
 * @see {@link StringDecoder}
 *
 * @author Nitesh Kant
 */
public class StringMessageConfigurator implements PipelineConfigurator<String, String> {

    private final Charset inputCharset;
    private final Charset outputCharset;

    public StringMessageConfigurator() {
        this(Charset.defaultCharset(), Charset.defaultCharset());
    }

    public StringMessageConfigurator(Charset inputCharset, Charset outputCharset) {
        this.inputCharset = inputCharset;
        this.outputCharset = outputCharset;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new StringDecoder(outputCharset))
                .addLast(new StringEncoder(inputCharset));
    }
}
