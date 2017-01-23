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
	public boolean isDisabled() {
		return disabled;
	}
	
	
}
