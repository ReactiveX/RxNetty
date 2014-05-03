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
package io.reactivex.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.reactivex.netty.pipeline.PipelineConfigurator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RxEventPipelineConfigurator implements PipelineConfigurator<RemoteRxEvent, RemoteRxEvent>{

    private static final Logger logger = LoggerFactory.getLogger(RxEventPipelineConfigurator.class);
	private static final byte PROTOCOL_VERSION = 1;
	
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
                        int protocolVersion = byteBuf.readByte();
                        if (protocolVersion != PROTOCOL_VERSION){
                        	throw new RuntimeException("Unsupported protocol version: "+protocolVersion);
                        }
                        int observableNameLength = byteBuf.readByte();
                        String observableName = null; 
                        if (observableNameLength > 0){
                        	// read name
                        	observableName = new String(byteBuf.readBytes(observableNameLength).array());
                        }
                        int operation = byteBuf.readByte();
                        RemoteRxEvent.Type type = null;
                        Map<String,String> subscribeParams = null;
                        byte[] valueData = null;
                        if (operation == 1){
                        	logger.debug("READ request for RemoteRxEvent: next");
                        	type = RemoteRxEvent.Type.next;
                        	valueData = new byte[byteBuf.readableBytes()];
                            byteBuf.readBytes(valueData);
                        }else if (operation == 2){
                        	logger.debug("READ request for RemoteRxEvent: error");
                        	type = RemoteRxEvent.Type.error;
                        	valueData = new byte[byteBuf.readableBytes()];
                            byteBuf.readBytes(valueData);
                        }else if (operation == 3){
                        	logger.debug("READ request for RemoteRxEvent: completed");
                        	type = RemoteRxEvent.Type.completed;
                        }else if (operation == 4){
                        	logger.debug("READ request for RemoteRxEvent: subscribed");
                        	type = RemoteRxEvent.Type.subscribed;
                        	// read subscribe parameters
                        	int subscribeParamsLength = byteBuf.readInt();
                        	if (subscribeParamsLength > 0){
                        		// read byte into map
                        		byte[] subscribeParamsBytes = new byte[subscribeParamsLength];
                        		byteBuf.readBytes(subscribeParamsBytes);
                        		subscribeParams = fromBytesToMap(subscribeParamsBytes);
                        	}
                        }else if (operation == 5){
                        	logger.debug("READ request for RemoteRxEvent: unsubscribed");
                        	type = RemoteRxEvent.Type.unsubscribed;
                        }else{
                        	throw new RuntimeException("operation: "+operation+" not support.");
                        }
                        handled = true;
                        byteBuf.release();
                        ctx.fireChannelRead(new RemoteRxEvent(observableName, type, valueData, subscribeParams));                        
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
					ByteBuf buf = ctx.alloc().buffer();
					buf.writeByte(PROTOCOL_VERSION);
					RemoteRxEvent event = (RemoteRxEvent) msg;
					String observableName = event.getName();
					if (observableName != null && !observableName.isEmpty()){
						// write length
						int nameLength = observableName.length();
						if (nameLength < 127){
							buf.writeByte(nameLength);
							buf.writeBytes(observableName.getBytes());
						}else{
							throw new RuntimeException("observableName "+observableName+
									" exceeds max limit of 127 characters");
						}
					}else{
						// no name provided, write 0 bytes for name length
						buf.writeByte(0);
					}
					if (event.getType() == RemoteRxEvent.Type.next){
                    	logger.debug("WRITE request for RemoteRxEvent: next");
						buf.writeByte(1);
						buf.writeBytes(event.getData());
						super.write(ctx, buf, promise);
					}else if (event.getType() == RemoteRxEvent.Type.error){
                    	logger.debug("WRITE request for RemoteRxEvent: error");
						buf.writeByte(2);
						buf.writeBytes(event.getData());
						super.write(ctx, buf, promise);
					}else if (event.getType() == RemoteRxEvent.Type.completed){
                    	logger.debug("WRITE request for RemoteRxEvent: completed");
						buf.writeByte(3);
						super.write(ctx, buf, promise);
						super.flush(ctx); // TODO why do I need to explicitly call flush for this to work??? empty data??
					}else if (event.getType() == RemoteRxEvent.Type.subscribed){
                    	logger.debug("WRITE request for RemoteRxEvent: subscribed");
						buf.writeByte(4);
						Map<String,String> subscribeParameters = event.getSubscribeParameters();
						if (subscribeParameters != null && !subscribeParameters.isEmpty()){
							byte[] subscribeBytes = fromMapToBytes(subscribeParameters);
							buf.writeInt(subscribeBytes.length); // write int for length
							buf.writeBytes(subscribeBytes); // write data
						}else{
							buf.writeInt(0); // no data
						}
						super.write(ctx, buf, promise);
						super.flush(ctx);
					}else if (event.getType() == RemoteRxEvent.Type.unsubscribed){
                    	logger.debug("WRITE request for RemoteRxEvent: unsubscribed");
						buf.writeByte(5);
						super.write(ctx, buf, promise);
						super.flush(ctx); // TODO why do I need to explicitly call flush for this to work??? empty data??
					}
				}else{
					super.write(ctx, msg, promise);
				}
			}
			
		});
		
	}
	
	@SuppressWarnings("unchecked")
	static Map<String,String> fromBytesToMap(byte[] bytes){
		Map<String,String> map = null;
		ByteArrayInputStream bis = null;
		ObjectInput in = null;
		try{
			bis = new ByteArrayInputStream(bytes);
			in = new ObjectInputStream(bis);
			map = (Map<String,String>) in.readObject();
			
		}catch(IOException e){
			throw new RuntimeException(e);
		}catch(ClassNotFoundException e1){
			throw new RuntimeException(e1);
		}finally{
			try {
				if (bis != null){bis.close();}
				if (in != null){in.close();}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return map;
	}
	
	static byte[] fromMapToBytes(Map<String,String> map){
		ByteArrayOutputStream baos = null;
		ObjectOutput out = null;
		try{
			baos = new ByteArrayOutputStream();
			out = new ObjectOutputStream(baos);   
			out.writeObject(map);
		}catch(IOException e){
			throw new RuntimeException(e);
		}finally{
			try{
				if (out != null){out.close();}
				if (baos != null){baos.close();}
			}catch(IOException e1){
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
		}
		return baos.toByteArray();
	}
	
}
