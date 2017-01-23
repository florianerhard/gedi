package gedi.oml.remote.Pipeline;

import gedi.remote.Protocol;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

public class TrackProtocol implements Protocol {

	
	
	@Override
	public String getName() {
		return "tracks";
	}

	@Override
	public void setCodecs(ChannelPipeline pipeline) {
		pipeline.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
		pipeline.addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
		addDefaultCodecs(pipeline);
	}

	@Override
	public int getDefaultPort() {
		return 58315;
	}

	
	@Override
	public String toString() {
		return getName();
	}
	
}

