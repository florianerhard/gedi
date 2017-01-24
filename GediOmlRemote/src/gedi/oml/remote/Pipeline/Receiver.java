/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.oml.remote.Pipeline;

import gedi.core.data.mapper.DisablingGenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

@GenomicRegionDataMapping(fromType=Void.class,toType=Object.class)
public class Receiver extends ChannelInboundHandlerAdapter implements DisablingGenomicRegionDataMapper<Void,Object>{

	
	private String id;
	private String server;
	
	private boolean disabled = true;
	
	
	
	public Receiver(String on) {
		this.server = on;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public boolean isDisabled() {
		return disabled;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public Object map(ReferenceSequence reference, GenomicRegion region,
			PixelLocationMapping pixelMapping, Void data) {
		
		
		synchronized (location) {
			location.set(reference, region, pixelMapping.getXmapper());
		}
		
		try {
			for(;;) {
				RemotePipelineData re = answer.take();
				
				synchronized (location) {
					boolean thisCorrect = reference.equals(location.getReference())  && region.equals(location.getRegion());
					boolean answerCorrect = re.getData().getReference().equals(location.getReference())  && re.getData().getRegion().equals(location.getRegion());
					if (thisCorrect) {
						if (answerCorrect) {
							Object ret = re.getData().getData();
							return ret;
						}
						// otherwise, the answer is not interesting anymore, wait for next answer
					} else {
						if (answerCorrect)
							answer.offer(re);
						else
							return null;
//						throw new RuntimeException("This should not happen as the thread is supposed to be interrupted already!");
					}
				}
			}
			
		} catch (InterruptedException e) {
			return null;
		}
	}

	
	
	private static final Logger log = Logger.getLogger( Receiver.class.getName() );

	private MutableReferenceGenomicRegion location = new MutableReferenceGenomicRegion();
	private LinkedBlockingQueue<RemotePipelineData> answer = new LinkedBlockingQueue<RemotePipelineData>(1);

	

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (!(msg instanceof RemotePipelineData)) {
			ctx.fireChannelRead(msg);
			return;
		}
		
		RemotePipelineData data = (RemotePipelineData) msg;
		
		boolean release = true;
        try {
        	
    	
    		synchronized (location) {
				if (data.getId().equals(id))
					answer.offer(data);
				else {
					release = false;
					ctx.fireChannelRead(msg);
				}
				
			}
        	
        } finally {
            if (release) {
                ReferenceCountUtil.release(msg);
            }
        }
	}

	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.log(Level.SEVERE, "Receiver caught exception!", cause);
	}
	
}
