package io.reactivex.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.reactivex.netty.pipeline.PipelineConfigurator;


public class RxEventPipelineConfigurator implements PipelineConfigurator<RemoteRxEvent, RemoteRxEvent>{

	@Override
	public void configureNewPipeline(ChannelPipeline pipeline) {
		pipeline.addLast(new ChannelDuplexHandler(){

			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg)
					throws Exception {
				boolean handled = false;
				if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
                    ByteBuf byteBuf = (ByteBuf) msg;
                    if (byteBuf.isReadable()) {
                        int readableBytes = byteBuf.readableBytes();
                        int operation = byteBuf.readByte();
                        RemoteRxEvent.Type type = null;
                        byte[] valueData = null;
                        if (operation == 1){
                        	type = RemoteRxEvent.Type.next;
                        	valueData = new byte[readableBytes-1];
                            byteBuf.readBytes(valueData);
                        }else if (operation == 2){
                        	type = RemoteRxEvent.Type.error;
                        	valueData = new byte[readableBytes-1];
                            byteBuf.readBytes(valueData);
                        }else if (operation == 3){
                        	type = RemoteRxEvent.Type.completed;
                        }else if (operation == 4){
                        	type = RemoteRxEvent.Type.subscribed;
                        }else if (operation == 5){
                        	type = RemoteRxEvent.Type.unsubscribed;
                        }
                        handled = true;
                        ctx.fireChannelRead(new RemoteRxEvent(valueData, type));                        
                    }
				}
				if (!handled){
					super.channelRead(ctx, msg);
				}
			}

			@Override
			public void write(ChannelHandlerContext ctx, Object msg,
					ChannelPromise promise) throws Exception {
				if (msg instanceof RemoteRxEvent){
					RemoteRxEvent event = (RemoteRxEvent) msg;
					if (event.getType() == RemoteRxEvent.Type.next){
						ByteBuf buf = ctx.alloc().buffer().writeByte(1);
						buf.writeBytes(event.getData());
						super.write(ctx, buf, promise);
					}else if (event.getType() == RemoteRxEvent.Type.error){
						ByteBuf buf = ctx.alloc().buffer().writeByte(2);
						buf.writeBytes(event.getData());
						super.write(ctx, buf, promise);
					}else if (event.getType() == RemoteRxEvent.Type.completed){
						ByteBuf buf = ctx.alloc().buffer().writeByte(3);
						super.write(ctx, buf, promise);
						super.flush(ctx); // TODO why do I need to explicitly call flush for this to work??? empty data??
					}else if (event.getType() == RemoteRxEvent.Type.subscribed){
						ByteBuf buf = ctx.alloc().buffer().writeByte(4);
						super.write(ctx, buf, promise);
						super.flush(ctx);
					}else if (event.getType() == RemoteRxEvent.Type.unsubscribed){
						ByteBuf buf = ctx.alloc().buffer().writeByte(5);
						super.write(ctx, buf, promise);
						super.flush(ctx); // TODO why do I need to explicitly call flush for this to work??? empty data??
					}
				}else{
					super.write(ctx, msg, promise);
				}
			}
			
		});
		
	}
	
}
