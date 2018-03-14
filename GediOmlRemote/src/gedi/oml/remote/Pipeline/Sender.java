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
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import io.netty.channel.ChannelHandlerContext;

@GenomicRegionDataMapping(fromType=Object.class,toType=Void.class)
public class Sender implements DisablingGenomicRegionDataMapper<Object,Void> {

	private String id;
	private ChannelHandlerContext ctx;
	
	private boolean disabled = false;
	
	public void setChannelHandlerContext(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}
	
	public String getInput() {
		return id;
	}
	
	public void setInput(String id) {
		this.id = id;
	}

	@Override
	public Void map(ReferenceSequence reference, GenomicRegion region,
			PixelLocationMapping pixelMapping, Object data) {
		ctx.writeAndFlush(new RemotePipelineData(id, new ImmutableReferenceGenomicRegion(reference, region,data)));
		return null;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	@Override
	public boolean isDisabled(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping) {
		return disabled;
	}
	
	
}
